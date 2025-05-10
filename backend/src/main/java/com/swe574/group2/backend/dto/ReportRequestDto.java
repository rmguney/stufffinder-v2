package com.swe574.group2.backend.dto;

import com.swe574.group2.backend.enums.ReportReason;
import com.swe574.group2.backend.enums.TargetType;

import lombok.Data;

@Data
public class ReportRequestDto {
    private TargetType targetType;
    private Long targetId;
    private ReportReason reason;
    private String notes;
}
