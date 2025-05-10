package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.ReportRequestDto;
import com.swe574.group2.backend.dto.ReportResponseDto;
import com.swe574.group2.backend.dto.ReportResolutionDto;
import com.swe574.group2.backend.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Submit a new report. Any authenticated user can report content.
     */
    @PostMapping("/report/submit")
    public ResponseEntity<ReportResponseDto> submitReport(
            @RequestBody ReportRequestDto request,
            Authentication authentication) {
        String reporterEmail = authentication.getName();
        ReportResponseDto dto = reportService.submitReport(request, reporterEmail);
        return ResponseEntity.ok(dto);
    }

    /**
     * List all reports with paging and filters. Admin only.
     */
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportResponseDto>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReportResponseDto> reportPage = reportService.listReports(pageable);
        return ResponseEntity.ok(reportPage);
    }

    /**
     * Get Report by Target | POST, COMMENT, PROFILE
     * 
     * @param targetType @see TargetType enum as string
     * @param id         regarding object id
     */
    @GetMapping("/reports/{targetType}/{id}")
    public ResponseEntity<List<ReportResponseDto>> listReportByTarget(
            @PathVariable("targetType") String targetType,
            @PathVariable("id") Long id) {
        List<ReportResponseDto> reportResponses = reportService.listReportByTarget(targetType, id);

        return ResponseEntity.ok(reportResponses);
    }

    /**
     * Resolve or dismiss a report. Admin only.
     * 
     * @param id     the report ID
     * @param action either "RESOLVED" or "DISMISSED"
     */
    @PutMapping("/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponseDto> resolveReport(
            @PathVariable("id") Long id,
            @RequestBody ReportResolutionDto resolutionDto,
            Authentication authentication) {
        String adminEmail = authentication.getName();
        ReportResponseDto dto = reportService.resolveReport(id, resolutionDto, adminEmail);
        return ResponseEntity.ok(dto);
    }
}
