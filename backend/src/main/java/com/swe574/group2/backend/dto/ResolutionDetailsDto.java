package com.swe574.group2.backend.dto;

import lombok.Data;
import java.util.List;

import java.time.LocalDateTime;

@Data
public class ResolutionDetailsDto {
    private Long id;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MediaFileDto> mediaFiles; // Added media files

    private Long postId;

    public ResolutionDetailsDto(Long id, String description, LocalDateTime createdAt, LocalDateTime updatedAt, 
                           List<MediaFileDto> mediaFiles, Long postId) {
        this.id = id;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.mediaFiles = mediaFiles;
        this.postId = postId;
    }

    public ResolutionDetailsDto() {
    }
}