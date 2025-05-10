package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dto.ReportRequestDto;
import com.swe574.group2.backend.dto.ReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ReportService {
    ReportResponseDto submitReport(ReportRequestDto request, String reporterEmail);
    Page<ReportResponseDto> listReports(Pageable pageable);
    List<ReportResponseDto> listReportByTarget(String targetType, Long id);
    ReportResponseDto resolveReport(Long reportId, com.swe574.group2.backend.dto.ReportResolutionDto resolutionDto, String adminEmail);
}
