package com.cyberassessment.service;

import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.dto.AuditDto;
import com.cyberassessment.dto.AuditProjectDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.AuditProjectRepository;
import com.cyberassessment.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditProjectService {
    private final AuditProjectRepository auditProjectRepository;
    private final ApplicationRepository applicationRepository;
    private final AuditRepository auditRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public static AuditProjectDto toDto(AuditProject p) {
        User createdBy = p.getCreatedBy();
        List<ApplicationDto> apps = p.getApplications().stream().map(ApplicationService::toDto).toList();
        List<AuditDto> audits = p.getAudits().stream().map(AuditService::toDto).toList();
        long complete = p.getAudits().stream().filter(a -> a.getStatus() == AuditStatus.COMPLETE).count();
        return AuditProjectDto.builder()
                .id(p.getId())
                .name(p.getName())
                .frameworkTag(p.getFrameworkTag())
                .year(p.getYear())
                .notes(p.getNotes())
                .startsAt(p.getStartsAt())
                .dueAt(p.getDueAt())
                .status(p.getStatus())
                .createdByUserId(createdBy != null ? createdBy.getId() : null)
                .createdByEmail(createdBy != null ? createdBy.getEmail() : null)
                .createdAt(p.getCreatedAt())
                .scopedApplications(apps)
                .audits(audits)
                .totalAudits(audits.size())
                .completeAudits(complete)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditProjectDto> list() {
        return auditProjectRepository.findAllByOrderByCreatedAtDesc().stream().map(AuditProjectService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AuditProjectDto get(Long projectId) {
        return auditProjectRepository.findById(projectId).map(AuditProjectService::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Audit project not found"));
    }

    @Transactional
    public AuditProjectDto create(String name, String frameworkTag, Integer year, String notes, Instant startsAt, Instant dueAt, List<Long> applicationIds) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can create audit projects");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (year == null) {
            throw new IllegalArgumentException("year is required");
        }
        if (applicationIds == null || applicationIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one application");
        }
        List<Application> applications = applicationRepository.findAllById(applicationIds);
        if (applications.size() != applicationIds.size()) {
            throw new IllegalArgumentException("One or more applications were not found");
        }
        AuditProject project = AuditProject.builder()
                .name(name.trim())
                .frameworkTag(frameworkTag)
                .year(year)
                .notes(notes)
                .startsAt(startsAt)
                .dueAt(dueAt)
                .status(AuditProjectStatus.ACTIVE)
                .createdBy(currentUserService.getCurrentUser().orElse(null))
                .build();
        project.setApplications(new ArrayList<>(applications));
        project = auditProjectRepository.save(project);
        Long projectId = project.getId();

        for (Application app : applications) {
            if (auditRepository.findByApplicationIdAndYear(app.getId(), year).isPresent()) {
                continue;
            }
            auditService.create(app.getId(), year, dueAt, projectId);
        }
        List<AuditDto> audits = auditRepository.findAll().stream()
                .filter(a -> a.getAuditProject() != null && a.getAuditProject().getId().equals(projectId))
                .map(AuditService::toDto)
                .toList();
        return AuditProjectDto.builder()
                .id(projectId)
                .name(project.getName())
                .frameworkTag(project.getFrameworkTag())
                .year(project.getYear())
                .notes(project.getNotes())
                .startsAt(project.getStartsAt())
                .dueAt(project.getDueAt())
                .status(project.getStatus())
                .createdByUserId(project.getCreatedBy() != null ? project.getCreatedBy().getId() : null)
                .createdByEmail(project.getCreatedBy() != null ? project.getCreatedBy().getEmail() : null)
                .createdAt(project.getCreatedAt())
                .scopedApplications(applications.stream().map(ApplicationService::toDto).toList())
                .audits(audits)
                .totalAudits(audits.size())
                .completeAudits(audits.stream().filter(a -> a.getStatus() == AuditStatus.COMPLETE).count())
                .build();
    }
}
