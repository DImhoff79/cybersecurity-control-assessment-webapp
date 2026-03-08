package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditYearSummaryDto;
import com.cyberassessment.dto.AuditorDashboardDto;
import com.cyberassessment.dto.ReportSummaryDto;
import com.cyberassessment.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportSummaryDto summary() {
        return reportService.getSummary();
    }

    @GetMapping("/by-year")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditYearSummaryDto> byYear() {
        return reportService.byYear();
    }

    @GetMapping("/audits.csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportAuditsCsv() {
        String csv = reportService.auditsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audits-report.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/auditor-dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public AuditorDashboardDto auditorDashboard() {
        return reportService.auditorDashboard();
    }
}
