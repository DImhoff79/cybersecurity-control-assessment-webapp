package com.cyberassessment.service;

import com.cyberassessment.dto.RiskControlLinkDto;
import com.cyberassessment.dto.RiskKpiDto;
import com.cyberassessment.dto.RiskRegisterItemDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskRegisterItemRepository riskRegisterItemRepository;
    private final RiskControlLinkRepository riskControlLinkRepository;
    private final RiskFindingLinkRepository riskFindingLinkRepository;
    private final RiskExceptionLinkRepository riskExceptionLinkRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ControlRepository controlRepository;
    private final FindingRepository findingRepository;
    private final ControlExceptionRepository controlExceptionRepository;
    private final RemediationPlanRepository remediationPlanRepository;
    private final RemediationActionRepository remediationActionRepository;
    private final CurrentUserService currentUserService;

    public static RiskRegisterItemDto toDto(RiskRegisterItem row) {
        return RiskRegisterItemDto.builder()
                .id(row.getId())
                .title(row.getTitle())
                .description(row.getDescription())
                .businessImpact(row.getBusinessImpact())
                .likelihoodScore(row.getLikelihoodScore())
                .impactScore(row.getImpactScore())
                .inherentRiskScore(row.getInherentRiskScore())
                .residualRiskScore(row.getResidualRiskScore())
                .status(row.getStatus())
                .ownerUserId(row.getOwner() != null ? row.getOwner().getId() : null)
                .ownerEmail(row.getOwner() != null ? row.getOwner().getEmail() : null)
                .applicationId(row.getApplication() != null ? row.getApplication().getId() : null)
                .applicationName(row.getApplication() != null ? row.getApplication().getName() : null)
                .targetCloseAt(row.getTargetCloseAt())
                .closedAt(row.getClosedAt())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    public static RiskControlLinkDto toDto(RiskControlLink row) {
        return RiskControlLinkDto.builder()
                .id(row.getId())
                .riskId(row.getRisk().getId())
                .controlId(row.getControl().getId())
                .controlControlId(row.getControl().getControlId())
                .controlName(row.getControl().getName())
                .notes(row.getNotes())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RiskRegisterItemDto> list() {
        return riskRegisterItemRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(RiskService::toDto)
                .toList();
    }

    @Transactional
    public RiskRegisterItemDto create(
            String title,
            String description,
            String businessImpact,
            Integer likelihoodScore,
            Integer impactScore,
            Long ownerUserId,
            Long applicationId,
            Instant targetCloseAt
    ) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Risk title is required");
        int likelihood = validateScore(likelihoodScore, "likelihoodScore");
        int impact = validateScore(impactScore, "impactScore");
        User owner = ownerUserId == null ? null : userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found: " + ownerUserId));
        Application app = applicationId == null ? null : applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        User actor = currentUserService.getCurrentUser().orElse(null);
        RiskRegisterItem row = riskRegisterItemRepository.save(RiskRegisterItem.builder()
                .title(title.trim())
                .description(description)
                .businessImpact(businessImpact)
                .likelihoodScore(likelihood)
                .impactScore(impact)
                .inherentRiskScore(likelihood * impact)
                .residualRiskScore(likelihood * impact)
                .owner(owner)
                .application(app)
                .status(RiskStatus.OPEN)
                .targetCloseAt(targetCloseAt)
                .createdBy(actor)
                .build());
        return toDto(row);
    }

    @Transactional
    public RiskRegisterItemDto update(
            Long riskId,
            String title,
            String description,
            String businessImpact,
            Integer likelihoodScore,
            Integer impactScore,
            Long ownerUserId,
            RiskStatus status,
            Integer residualRiskScore,
            Instant targetCloseAt
    ) {
        RiskRegisterItem row = riskRegisterItemRepository.findById(riskId)
                .orElseThrow(() -> new IllegalArgumentException("Risk not found: " + riskId));
        if (title != null && !title.isBlank()) row.setTitle(title.trim());
        if (description != null) row.setDescription(description);
        if (businessImpact != null) row.setBusinessImpact(businessImpact);
        if (likelihoodScore != null) row.setLikelihoodScore(validateScore(likelihoodScore, "likelihoodScore"));
        if (impactScore != null) row.setImpactScore(validateScore(impactScore, "impactScore"));
        row.setInherentRiskScore(row.getLikelihoodScore() * row.getImpactScore());
        if (ownerUserId != null) {
            User owner = userRepository.findById(ownerUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Owner user not found: " + ownerUserId));
            row.setOwner(owner);
        }
        if (status != null) {
            row.setStatus(status);
            if (status == RiskStatus.CLOSED && row.getClosedAt() == null) {
                row.setClosedAt(Instant.now());
            }
        }
        if (residualRiskScore != null) row.setResidualRiskScore(Math.max(0, residualRiskScore));
        if (targetCloseAt != null) row.setTargetCloseAt(targetCloseAt);
        return toDto(riskRegisterItemRepository.save(row));
    }

    @Transactional
    public RiskControlLinkDto linkControl(Long riskId, Long controlId, String notes) {
        RiskRegisterItem risk = riskRegisterItemRepository.findById(riskId)
                .orElseThrow(() -> new IllegalArgumentException("Risk not found: " + riskId));
        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> new IllegalArgumentException("Control not found: " + controlId));
        RiskControlLink row = riskControlLinkRepository.save(RiskControlLink.builder()
                .risk(risk)
                .control(control)
                .notes(notes)
                .build());
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<RiskControlLinkDto> controls(Long riskId) {
        return riskControlLinkRepository.findByRiskIdOrderByIdDesc(riskId).stream()
                .map(RiskService::toDto)
                .toList();
    }

    @Transactional
    public void linkFinding(Long riskId, Long findingId) {
        RiskRegisterItem risk = riskRegisterItemRepository.findById(riskId)
                .orElseThrow(() -> new IllegalArgumentException("Risk not found: " + riskId));
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found: " + findingId));
        riskFindingLinkRepository.save(RiskFindingLink.builder().risk(risk).finding(finding).build());
    }

    @Transactional
    public void linkException(Long riskId, Long exceptionId) {
        RiskRegisterItem risk = riskRegisterItemRepository.findById(riskId)
                .orElseThrow(() -> new IllegalArgumentException("Risk not found: " + riskId));
        ControlExceptionRequest exception = controlExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new IllegalArgumentException("Exception not found: " + exceptionId));
        riskExceptionLinkRepository.save(RiskExceptionLink.builder().risk(risk).exceptionRequest(exception).build());
    }

    @Transactional(readOnly = true)
    public RiskKpiDto kpis() {
        long openRisks = riskRegisterItemRepository.countByStatusIn(List.of(RiskStatus.OPEN, RiskStatus.MONITORING));
        long highRisks = riskRegisterItemRepository.countByResidualRiskScoreGreaterThanEqual(12);
        long overdueRemediationActions = remediationActionRepository.countByStatusAndDueAtBefore(
                RemediationActionStatus.TODO,
                Instant.now()
        ) + remediationActionRepository.countByStatusAndDueAtBefore(RemediationActionStatus.IN_PROGRESS, Instant.now());
        long plansInProgress = remediationPlanRepository.countByStatus(RemediationPlanStatus.IN_PROGRESS);
        return RiskKpiDto.builder()
                .openRisks(openRisks)
                .highRisks(highRisks)
                .overdueRemediationActions(overdueRemediationActions)
                .plansInProgress(plansInProgress)
                .build();
    }

    private int validateScore(Integer value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        if (value < 1 || value > 5) throw new IllegalArgumentException(field + " must be between 1 and 5");
        return value;
    }
}
