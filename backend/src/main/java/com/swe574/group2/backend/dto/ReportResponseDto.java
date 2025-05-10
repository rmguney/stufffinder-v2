package com.swe574.group2.backend.dto;

import com.swe574.group2.backend.enums.ReportReason;
import com.swe574.group2.backend.enums.ReportStatus;
import com.swe574.group2.backend.enums.TargetType;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportResponseDto{
    private Long id;
    private String reporterUsername;
    private TargetType targetType;
    private String reportedUsername;
    private Long targetId;
    private Long postId;
    private ReportReason reason;
    private String notes;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private String resolvedByUsername;
    private LocalDateTime resolvedAt;
    private Boolean removeContent;
    private Boolean banUser;
    private Boolean sendWarning;
}
