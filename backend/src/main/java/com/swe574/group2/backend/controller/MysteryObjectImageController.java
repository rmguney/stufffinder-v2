package com.swe574.group2.backend.controller;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.swe574.group2.backend.dao.MysteryObjectRepository;
import com.swe574.group2.backend.entity.MysteryObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/mysteryObjects")
public class MysteryObjectImageController {

    private final MysteryObjectRepository mysteryObjectRepository;
    private final Storage storage;

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Autowired
    public MysteryObjectImageController(MysteryObjectRepository mysteryObjectRepository, Storage storage) {
        this.mysteryObjectRepository = mysteryObjectRepository;
        this.storage = storage;
    }

    @PostMapping("/{id}/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@PathVariable Long id, @RequestParam("image") MultipartFile imageFile) {
        return mysteryObjectRepository.findById(id).map(mysteryObject -> {
            try {
                // Generate a unique filename for the GCS object
                String fileName = "mystery-objects/" + UUID.randomUUID();
                
                // Create a BlobId and BlobInfo for the file
                BlobId blobId = BlobId.of(bucketName, fileName);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(imageFile.getContentType())
                    .setAcl(new ArrayList<>(Collections.singletonList(
                        Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))))
                    .build();
                
                // Upload the file to Google Cloud Storage
                storage.create(blobInfo, imageFile.getBytes());
                
                // Generate a URL for the uploaded image
                // This creates a publicly accessible URL that expires after 7 days
                //String imageUrl = storage.get(blobId).signUrl(7, TimeUnit.DAYS).toString();
                
                // For permanently public images, use this instead:
                String imageUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
                
                // Update the mystery object with the image URL
                mysteryObject.setImageUrl(imageUrl);
                // Set the binary image data to null to save space
                mysteryObject.setImage(imageFile.getBytes());
                mysteryObjectRepository.save(mysteryObject);

                Map<String, String> response = new HashMap<>();
                response.put("message", "Image uploaded successfully!");
                response.put("imageUrl", imageUrl);
                return ResponseEntity.ok(response);
            } catch (IOException e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Failed to upload image: " + e.getMessage());
                return ResponseEntity.status(500).body(errorResponse);
            }
        }).orElse(ResponseEntity.status(404).body(Map.of("message", "Mystery Object not found")));
    }
}