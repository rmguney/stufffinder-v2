package com.swe574.group2.backend.controller;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dto.CommentCreateDto;
import com.swe574.group2.backend.dto.CommentDetailsDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.MediaFile;
import com.swe574.group2.backend.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final MediaFileRepository mediaFileRepository;
    private final Storage storage;
    
    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Autowired
    public CommentController(CommentService commentService, 
                           CommentRepository commentRepository,
                           MediaFileRepository mediaFileRepository,
                           Storage storage) {
        this.commentService = commentService;
        this.commentRepository = commentRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.storage = storage;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Long>> createComment(@RequestBody CommentCreateDto commentCreateDto, @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Long> response = commentService.createComment(commentCreateDto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/get/{postId}")
    public ResponseEntity<List<CommentDetailsDto>> getCommentsForPost(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        List<CommentDetailsDto> comments = commentService.getCommentsForPost(postId, username);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/upvote/{commentId}")
    public ResponseEntity<Map<String, Long>> upvoteComment(@PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Long> response = commentService.upvoteComment(commentId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/downvote/{commentId}")
    public ResponseEntity<Map<String, Long>> downvoteComment(@PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Long> response = commentService.downvoteComment(commentId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Upload media file to a comment
     */
    @PostMapping("/{commentId}/upload-media")
    public ResponseEntity<Map<String, Object>> uploadMediaToComment(
            @PathVariable Long commentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return commentRepository.findById(commentId).map(comment -> {
            try {
                // Verify that the current user is the author of the comment
                /* if (!comment.getUser().getUsername().equals(userDetails.getUsername())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("message", "You can only upload media to your own comments");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                } */
                
                // Generate a unique filename for the GCS object
                String fileName = "comment-media/" + UUID.randomUUID();
                
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
                
                // Create new MediaFile entity
                MediaFile mediaFile = new MediaFile();
                mediaFile.setComment(comment); // Link media to comment instead of mystery object
                mediaFile.setFileName(file.getOriginalFilename());
                mediaFile.setFileType(file.getContentType());
                mediaFile.setFileUrl(fileUrl);
                mediaFile.setFileData(file.getBytes()); // Store locally as well for faster access
                
                // Save the media file
                MediaFile savedFile = mediaFileRepository.save(mediaFile);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Media uploaded successfully!");
                response.put("id", savedFile.getId());
                response.put("fileName", savedFile.getFileName());
                response.put("fileType", savedFile.getFileType());
                response.put("fileUrl", fileUrl);
                return ResponseEntity.ok(response);
                
            } catch (IOException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Failed to upload media: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Comment not found")));
    }
    
    /**
     * Get media files for a comment
     */
    @GetMapping("/{commentId}/media")
    public ResponseEntity<List<Map<String, Object>>> getMediaForComment(@PathVariable Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        
        List<MediaFile> mediaFiles = mediaFileRepository.findByCommentId(commentId);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (MediaFile media : mediaFiles) {
            Map<String, Object> mediaInfo = new HashMap<>();
            mediaInfo.put("id", media.getId());
            mediaInfo.put("fileName", media.getFileName());
            mediaInfo.put("fileType", media.getFileType());
            mediaInfo.put("fileUrl", media.getFileUrl());
            mediaInfo.put("createdAt", media.getCreatedAt());
            response.add(mediaInfo);
        }
        
        return ResponseEntity.ok(response);
    }
}