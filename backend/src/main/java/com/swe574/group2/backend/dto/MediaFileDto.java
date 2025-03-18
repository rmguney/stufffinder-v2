package com.swe574.group2.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MediaFileDto {
    private Long id;
    private String fileName;
    private String fileType;
    private LocalDateTime createdAt;
}