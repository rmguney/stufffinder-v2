package com.swe574.group2.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dto.PostCreationDto;
import com.swe574.group2.backend.dto.PostDetailsDto;
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.dto.SearchResultDto;
import com.swe574.group2.backend.entity.MediaFile;
import com.swe574.group2.backend.entity.MysteryObject;
import com.swe574.group2.backend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final Storage storage;
    private final MediaFileRepository mediaFileRepository;

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Autowired
    public PostController(PostService postService, Storage storage, MediaFileRepository mediaFileRepository) {
        this.postService = postService;
        this.storage = storage;
        this.mediaFileRepository = mediaFileRepository;
    }

    // New JSON endpoint for post creation
    @PostMapping(value = "/create-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> createPostJson(
            @RequestBody Map<String, Object> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        
        String title = (String) requestBody.get("title");
        String content = (String) requestBody.get("content");
        List<String> tags = requestBody.containsKey("tags") ? 
            (List<String>) requestBody.get("tags") : Collections.emptyList();
        
        ObjectMapper objectMapper = new ObjectMapper();
        MysteryObject mysteryObject = objectMapper.convertValue(
            requestBody.get("mysteryObject"), MysteryObject.class);
        
        PostCreationDto postCreationDto = new PostCreationDto();
        postCreationDto.setTitle(title);
        postCreationDto.setContent(content);
        postCreationDto.setTags(new HashSet<>(tags));
        postCreationDto.setMysteryObject(mysteryObject);
        
        Map<String, Long> response = postService.createPost(postCreationDto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Long>> createPost(
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "tags", required = false) List<String> tags,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @RequestPart("mysteryObject") String mysteryObjectJson,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        
        ObjectMapper objectMapper = new ObjectMapper();
        MysteryObject mysteryObject = objectMapper.readValue(mysteryObjectJson, MysteryObject.class);
        
        PostCreationDto postCreationDto = new PostCreationDto();
        postCreationDto.setTitle(title);
        postCreationDto.setContent(content);
        postCreationDto.setTags(new HashSet<>(tags != null ? tags : Collections.emptyList()));
        postCreationDto.setImage(image);
        postCreationDto.setMysteryObject(mysteryObject);
        
        Map<String, Long> response = postService.createPost(postCreationDto, userDetails.getUsername());
        
        // Handle legacy single image upload for backward compatibility
        if (image != null && !image.isEmpty()) {
            uploadImageToStorage(response.get("mysteryObjectId"), image);
        }
        
        // Process multiple media files if provided
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile mediaFile : mediaFiles) {
                if (!mediaFile.isEmpty()) {
                    uploadMediaFileToStorage(response.get("mysteryObjectId"), mediaFile);
                }
            }
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{postId}/mysteryObjects/{mysteryObjectId}/set-image")
    public ResponseEntity<Map<String, String>> setMysteryObjectImage(
            @PathVariable Long postId,
            @PathVariable Long mysteryObjectId,
            @RequestParam("file") MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        
        if (mysteryObjectId == null || image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid image or mystery object"));
        }

        // Get the mystery object
        MysteryObject mysteryObject = postService.getMysteryObjectById(mysteryObjectId);
        if (mysteryObject == null) {
            return ResponseEntity.notFound().build();
        }

        // Upload and set the image
        uploadImageToStorage(mysteryObjectId, image);
        
        return ResponseEntity.ok(Collections.singletonMap("status", "Image uploaded successfully"));
    }
    
    private void uploadImageToStorage(Long mysteryObjectId, MultipartFile image) throws IOException {
        if (mysteryObjectId == null || image == null || image.isEmpty()) {
            return;
        }

        // Get the mystery object
        MysteryObject mysteryObject = postService.getMysteryObjectById(mysteryObjectId);
        if (mysteryObject == null) {
            return;
        }

        // Generate a unique filename for the GCS object
        String fileName = "mystery-objects/" + UUID.randomUUID();
        
        // Create a BlobId and BlobInfo for the file
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(image.getContentType())
            .setAcl(new ArrayList<>(Collections.singletonList(
                Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))))
            .build();
        
        // Upload the file to Google Cloud Storage
        storage.create(blobInfo, image.getBytes());
        
        // Generate a URL for the uploaded image
        String imageUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
        
        // Update the mystery object with the image URL
        mysteryObject.setImageUrl(imageUrl);
        mysteryObject.setImage(image.getBytes());
        postService.saveMysteryObject(mysteryObject);
    }

    private void uploadMediaFileToStorage(Long mysteryObjectId, MultipartFile file) throws IOException {
        if (mysteryObjectId == null || file == null || file.isEmpty()) {
            return;
        }

        // Get the mystery object
        MysteryObject mysteryObject = postService.getMysteryObjectById(mysteryObjectId);
        if (mysteryObject == null) {
            return;
        }

        // Generate a unique filename for the GCS object
        String fileName = "mystery-objects/" + UUID.randomUUID();
        
        // Create a BlobId and BlobInfo for the file
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .setAcl(new ArrayList<>(Collections.singletonList(
                Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))))
            .build();
        
        // Upload the file to Google Cloud Storage
        storage.create(blobInfo, file.getBytes());
        
        // Generate a URL for the uploaded file
        String fileUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
        
        // Create and save new MediaFile entity
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMysteryObject(mysteryObject);
        mediaFile.setFileName(file.getOriginalFilename());
        mediaFile.setFileType(file.getContentType());
        mediaFile.setFileUrl(fileUrl);
        mediaFile.setFileData(file.getBytes());
        
        mediaFileRepository.save(mediaFile);
    }

    @GetMapping("/getForPostList")
    public Page<PostListDto> getPostsForPostList(Pageable pageable) {
        return postService.getAllPostsForPostList(pageable);
    }

    @GetMapping("/getForPostDetails/{postId}")
    public ResponseEntity<PostDetailsDto> getPostDetails(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        PostDetailsDto postDetailsDto = postService.getPostDetails(postId, username);
        return ResponseEntity.ok(postDetailsDto);
    }

    @PostMapping("/upvote/{postId}")
    public ResponseEntity<Map<String, Long>> upvotePost(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Long> response = postService.upvotePost(postId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/downvote/{postId}")
    public ResponseEntity<Map<String, Long>> downvotePost(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Long> response = postService.downvotePost(postId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}/markBestAnswer/{commentId}")
    public ResponseEntity<Map<String, Long>> markBestAnswer(@PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        boolean success = postService.markBestAnswer(postId, commentId, userDetails.getUsername());
        if (success) {
            Map<String, Long> response = Map.of("postId", postId, "commentId", commentId);
            return ResponseEntity.ok(response);
        } else {
            Map<String, Long> response = Map.of("postId", postId, "commentId", commentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    @GetMapping("/searchForPosts")
    public ResponseEntity<Page<SearchResultDto>> searchPosts(@RequestParam("q") String query, Pageable pageable) {
        Page<SearchResultDto> searchResults = postService.searchPosts(query, pageable);
        return ResponseEntity.ok(searchResults);
    }
}
