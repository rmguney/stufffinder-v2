package com.swe574.group2.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResolutionCreateDto {
    private String description;
    private Long postId;
    private List<Long> comments; // This should match the Svelte payload
}

