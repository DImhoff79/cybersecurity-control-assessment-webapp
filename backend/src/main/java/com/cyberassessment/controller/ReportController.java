package com.cyberassessment.controller;

import com.cyberassessment.dto.AuditYearSummaryDto;
import com.cyberassessment.dto.AuditTrendPointDto;
import com.cyberassessment.dto.AuditProjectSummaryDto;
import com.cyberassessment.dto.AuditorDashboardDto;
import com.cyberassessment.dto.AuditorSavedFilterDto;
import com.cyberassessment.dto.ReportScheduleDto;
import com.cyberassessment.dto.ReportScheduleUpsertRequest;
import com.cyberassessment.dto.ReportSummaryDto;
import com.cyberassessment.service.AuditorSavedFilterService;
import com.cyberassessment.service.ReportService;
import com.cyberassessment.service.ReportScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AuditorSavedFilterService auditorSavedFilterService;
    private final ReportScheduleService reportScheduleService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ReportSummaryDto summary() {
        return reportService.getSummary();
    }

    @GetMapping("/by-year")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public List<AuditYearSummaryDto> byYear() {
        return reportService.byYear();
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public List<AuditTrendPointDto> trends() {
        return reportService.trends();
    }

    @GetMapping("/by-project")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public List<AuditProjectSummaryDto> byProject() {
        return reportService.byProject();
    }

    @GetMapping("/audits.csv")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<String> exportAuditsCsv() {
        String csv = reportService.auditsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audits-report.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/recent-activity.csv")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<String> exportRecentActivityCsv(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false, defaultValue = "false") boolean noProjectOnly) {
        String csv = reportService.recentActivityCsv(category, search, projectId, noProjectOnly);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recent-activity.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/board-pack.pdf")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<byte[]> exportBoardPackPdf() {
        byte[] pdf = reportService.boardPackPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-board-pack.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/auditor-dashboard")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public AuditorDashboardDto auditorDashboard() {
        return reportService.auditorDashboard();
    }

    @GetMapping("/saved-filters")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public List<AuditorSavedFilterDto> savedFilters() {
        return auditorSavedFilterService.listForCurrentUser();
    }

    @PostMapping("/saved-filters")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public AuditorSavedFilterDto saveFilter(@RequestBody Map<String, Object> body) {
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        boolean shared = body.get("shared") != null && Boolean.TRUE.equals(body.get("shared"));
        Map<String, Object> filterState = body.containsKey("filterState")
                ? (Map<String, Object>) body.get("filterState")
                : Map.of();
        return auditorSavedFilterService.create(name, shared, filterState);
    }

    @DeleteMapping("/saved-filters/{id}")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public void deleteFilter(@PathVariable Long id) {
        auditorSavedFilterService.delete(id);
    }

    @GetMapping("/schedules")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public List<ReportScheduleDto> schedules() {
        return reportScheduleService.list();
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ReportScheduleDto createSchedule(@RequestBody ReportScheduleUpsertRequest request) {
        return reportScheduleService.create(request);
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public void deleteSchedule(@PathVariable Long id) {
        reportScheduleService.delete(id);
    }
}
