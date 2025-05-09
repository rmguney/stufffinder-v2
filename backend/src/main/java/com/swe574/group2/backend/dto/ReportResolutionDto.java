package com.swe574.group2.backend.dto;

import lombok.Data;

@Data
public class ReportResolutionDto {
    private String action; // "RESOLVED" or "DISMISSED"
    private String resolutionNote; // Optional
    private boolean removeContent;
    private boolean banUser;
    private boolean sendWarning;
}
