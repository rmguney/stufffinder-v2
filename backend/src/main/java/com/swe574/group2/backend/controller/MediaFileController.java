package com.swe574.group2.backend.controller;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dao.MysteryObjectRepository;
import com.swe574.group2.backend.entity.MediaFile;
import com.swe574.group2.backend.entity.MysteryObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/mysteryObjects")
public class MediaFileController {

    private final MysteryObjectRepository mysteryObjectRepository;
    private final MediaFileRepository mediaFileRepository;
    private final Storage storage;

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Autowired
    public MediaFileController(MysteryObjectRepository mysteryObjectRepository, 
                              MediaFileRepository mediaFileRepository,
                              Storage storage) {
        this.mysteryObjectRepository = mysteryObjectRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.storage = storage;
    }

    @PostMapping("/{id}/upload-media")
    public ResponseEntity<Map<String, Object>> uploadMedia(
            @PathVariable Long id, 
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return mysteryObjectRepository.findById(id).map(mysteryObject -> {
            try {
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
                
                // Create new MediaFile entity
                MediaFile mediaFile = new MediaFile();
                mediaFile.setMysteryObject(mysteryObject);
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
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Mystery Object not found")));
    }
    
    @GetMapping("/media/{mediaId}")
    public ResponseEntity<?> getMedia(@PathVariable Long mediaId) {
        return mediaFileRepository.findById(mediaId)
            .map(mediaFile -> {
                if (mediaFile.getFileUrl() != null) {
                    // Redirect to GCS URL
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.LOCATION, mediaFile.getFileUrl());
                    return new ResponseEntity<>(headers, HttpStatus.FOUND);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Media URL not found");
                }
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Media not found"));
    }
    
    @GetMapping("/{objectId}/media")
    public ResponseEntity<List<Map<String, Object>>> getMediaForObject(@PathVariable Long objectId) {
        if (!mysteryObjectRepository.existsById(objectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        
        List<MediaFile> mediaFiles = mediaFileRepository.findByMysteryObjectId(objectId);
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
    
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Map<String, String>> deleteMedia(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return mediaFileRepository.findById(mediaId)
            .map(mediaFile -> {
                try {
                    // Here you would add permission check to ensure user owns this media file
                    mediaFileRepository.delete(mediaFile);
                    return ResponseEntity.ok(Map.of("message", "Media deleted successfully"));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to delete media: " + e.getMessage()));
                }
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Media not found")));
    }
}
