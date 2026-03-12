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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        if (!currentUserService.isAdminOrAuditManager()) {
            throw new IllegalArgumentException("Only ADMIN or AUDIT_MANAGER can create audit projects");
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
            ensureAuditLinkedForProjectApplication(project, app, dueAt);
        }
        List<AuditDto> audits = auditRepository.findByAuditProjectId(projectId).stream()
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

    @Transactional
    public AuditProjectDto update(Long projectId, String name, String frameworkTag, Integer year, String notes, Instant startsAt, Instant dueAt, List<Long> applicationIds) {
        if (!currentUserService.isAdminOrAuditManager()) {
            throw new IllegalArgumentException("Only ADMIN or AUDIT_MANAGER can edit audit projects");
        }
        AuditProject project = auditProjectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Audit project not found"));
        if (name != null) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("name cannot be blank");
            }
            project.setName(name.trim());
        }
        if (frameworkTag != null) project.setFrameworkTag(frameworkTag);
        if (year != null) project.setYear(year);
        if (notes != null) project.setNotes(notes);
        if (startsAt != null) project.setStartsAt(startsAt);
        if (dueAt != null) project.setDueAt(dueAt);
        if (applicationIds != null) {
            List<Application> applications = applicationRepository.findAllById(applicationIds);
            if (applications.size() != applicationIds.size()) {
                throw new IllegalArgumentException("One or more applications were not found");
            }
            project.setApplications(new ArrayList<>(applications));
        }
        project = auditProjectRepository.save(project);

        // Keep linked audits aligned with the project's current scope + year.
        Set<Long> scopedAppIds = new HashSet<>(project.getApplications().stream().map(Application::getId).toList());
        List<Audit> linkedAudits = auditRepository.findByAuditProjectId(projectId);
        for (Audit linked : linkedAudits) {
            boolean appStillScoped = scopedAppIds.contains(linked.getApplication().getId());
            boolean yearMatches = Objects.equals(linked.getYear(), project.getYear());
            if (!appStillScoped || !yearMatches) {
                linked.setAuditProject(null);
            }
        }
        auditRepository.saveAll(linkedAudits);

        for (Application app : project.getApplications()) {
            ensureAuditLinkedForProjectApplication(project, app, project.getDueAt());
        }

        return toDto(project);
    }

    @Transactional
    public void delete(Long projectId) {
        if (!currentUserService.isAdminOrAuditManager()) {
            throw new IllegalArgumentException("Only ADMIN or AUDIT_MANAGER can delete audit projects");
        }
        AuditProject project = auditProjectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Audit project not found"));
        List<Audit> audits = auditRepository.findByAuditProjectId(projectId);
        for (Audit audit : audits) {
            audit.setAuditProject(null);
        }
        auditRepository.saveAll(audits);
        auditProjectRepository.delete(project);
    }

    private void ensureAuditLinkedForProjectApplication(AuditProject project, Application app, Instant dueAt) {
        Audit existing = auditRepository.findByApplicationIdAndYear(app.getId(), project.getYear()).orElse(null);
        Audit targetAudit;
        if (existing == null) {
            AuditDto created = auditService.create(app.getId(), project.getYear(), dueAt, project.getId());
            targetAudit = auditRepository.findById(created.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Created audit not found"));
        } else {
            if (existing.getAuditProject() != null && !Objects.equals(existing.getAuditProject().getId(), project.getId())) {
                throw new IllegalArgumentException(
                        "Application \"" + app.getName() + "\" already has a " + project.getYear()
                                + " audit in another project");
            }
            if (existing.getAuditProject() == null || !Objects.equals(existing.getAuditProject().getId(), project.getId())) {
                existing.setAuditProject(project);
            }
            if (existing.getDueAt() == null && dueAt != null) {
                existing.setDueAt(dueAt);
            }
            targetAudit = auditRepository.save(existing);
        }

        // Default assignment: route new project audits to each application's owner.
        if (app.getOwner() != null && (targetAudit.getAssignedTo() == null
                || !Objects.equals(targetAudit.getAssignedTo().getId(), app.getOwner().getId()))) {
            auditService.assign(targetAudit.getId(), app.getOwner().getId());
        }
    }
}
