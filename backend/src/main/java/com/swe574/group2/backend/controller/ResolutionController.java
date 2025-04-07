package com.swe574.group2.backend.controller;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.swe574.group2.backend.dao.ResolutionRepository;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dto.ResolutionCreateDto;
import com.swe574.group2.backend.dto.ResolutionDetailsDto;
import com.swe574.group2.backend.entity.Resolution;
import com.swe574.group2.backend.entity.MediaFile;
import com.swe574.group2.backend.service.ResolutionService;
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
@RequestMapping("/api/resolution")
public class ResolutionController {

    private final ResolutionService resolutionService;
    private final ResolutionRepository resolutionRepository;
    private final MediaFileRepository mediaFileRepository;
    private final Storage storage;
    
    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Autowired
    public ResolutionController(ResolutionService resolutionService, 
                           ResolutionRepository resolutionRepository,
                           MediaFileRepository mediaFileRepository,
                           Storage storage) {
        this.resolutionService = resolutionService;
        this.resolutionRepository = resolutionRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.storage = storage;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Long>> createResolution(@RequestBody ResolutionCreateDto resolutionCreateDto, @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Long> response = resolutionService.createResolution(resolutionCreateDto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/get/{postId}")
    public ResponseEntity<List<ResolutionDetailsDto>> getResolutionsForPost(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        List<ResolutionDetailsDto> resolutions = resolutionService.getResolutionsForPost(postId, username);
        return ResponseEntity.ok(resolutions);
    }
    
    /**
     * Upload media file to a resolution
     */
    @PostMapping("/{resolutionId}/upload-media")
    public ResponseEntity<Map<String, Object>> uploadMediaToResolution(
            @PathVariable Long resolutionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return resolutionRepository.findById(resolutionId).map(resolution -> {
            try {
                
                // Generate a unique filename for the GCS object
                String fileName = "resolution-media/" + UUID.randomUUID();
                
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
                mediaFile.setResolution(resolution); // Link media to resolution instead of mystery object and comment
                mediaFile.setFileName(file.getOriginalFilename());
                mediaFile.setFileType(file.getContentType());
                mediaFile.setFileUrl(fileUrl);
                
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
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Resolution not found")));
    }
    
    /**
     * Get media files for a resolution
     */
    @GetMapping("/{resolutionId}/media")
    public ResponseEntity<List<Map<String, Object>>> getMediaForResolution(@PathVariable Long resolutionId) {
        if (!resolutionRepository.existsById(resolutionId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        
        List<MediaFile> mediaFiles = mediaFileRepository.findByResolutionId(resolutionId);
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