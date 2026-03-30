package com.cyberassessment.service;

import com.cyberassessment.config.AuditSeedProperties;
import com.cyberassessment.dto.ApplicationAssessmentControlDto;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.RegulatoryScopeTag;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.util.RegulatoryScopeMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationAssessmentControlService {

    private final ApplicationRepository applicationRepository;
    private final ControlRepository controlRepository;
    private final CurrentUserService currentUserService;
    private final AuditSeedProperties auditSeedProperties;

    @Transactional(readOnly = true)
    public List<ApplicationAssessmentControlDto> listForApplication(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        assertCanReadApplication(app);
        List<Control> controls = controlRepository.findAllEnabledWithRegulatoryScopes();
        return controls.stream()
                .sorted(Comparator.comparing(Control::getControlId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(c -> toRow(c, app))
                .collect(Collectors.toList());
    }

    private void assertCanReadApplication(Application app) {
        var me = currentUserService.getCurrentUserOrThrow();
        if (currentUserService.isAdmin()
                || currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT)
                || currentUserService.hasPermission(UserPermission.APPLICATION_MANAGEMENT)) {
            return;
        }
        if (app.getOwner() != null && Objects.equals(app.getOwner().getId(), me.getId())) {
            return;
        }
        throw new IllegalArgumentException("Not allowed to view this application");
    }

    private ApplicationAssessmentControlDto toRow(Control c, Application app) {
        boolean applies = !auditSeedProperties.isRegulatoryScopeFilterEnabled()
                || RegulatoryScopeMatcher.controlAppliesToApplication(c, app);
        List<String> scopes = c.getRegulatoryScopes() == null
                ? List.of()
                : c.getRegulatoryScopes().stream().map(RegulatoryScopeTag::name).sorted().toList();
        return ApplicationAssessmentControlDto.builder()
                .id(c.getId())
                .controlId(c.getControlId())
                .name(c.getName())
                .framework(c.getFramework())
                .category(c.getCategory())
                .regulatoryScopes(scopes)
                .includedUnderCurrentFilter(applies)
                .build();
    }
}
