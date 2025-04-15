package com.swe574.group2.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ResolutionDto {
    private String description;
    private List<Long> contributingCommentIds;
    private LocalDateTime resolvedAt;
}
