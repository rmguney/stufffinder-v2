package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dao.MysteryObjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mysteryObjects")
@CrossOrigin(origins = {"http://localhost:4200", "https://swe573-frontend-594781402587.us-central1.run.app"})
public class MysteryObjectImageController {

    private final MysteryObjectRepository mysteryObjectRepository;

    public MysteryObjectImageController(MysteryObjectRepository mysteryObjectRepository) {
        this.mysteryObjectRepository = mysteryObjectRepository;
    }

    @PostMapping("/{id}/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@PathVariable Long id, @RequestParam("image") MultipartFile imageFile) {
        return mysteryObjectRepository.findById(id).map(mysteryObject -> {
            try {
                byte[] imageBytes = imageFile.getBytes();
                mysteryObject.setImage(imageBytes);
                mysteryObjectRepository.save(mysteryObject);

                // Return a JSON response instead of plain text
                Map<String, String> response = new HashMap<>();
                response.put("message", "Image uploaded successfully!");
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Failed to upload image");
                return ResponseEntity.status(500).body(errorResponse);
            }
        }).orElse(ResponseEntity.status(404).body(Map.of("message", "Mystery Object not found")));
    }
}
