package com.cyberassessment.service;

import com.cyberassessment.dto.*;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final RegulationRepository regulationRepository;
    private final ComplianceRequirementRepository complianceRequirementRepository;
    private final RequirementControlMappingRepository requirementControlMappingRepository;
    private final PolicyRequirementMappingRepository policyRequirementMappingRepository;
    private final ControlRepository controlRepository;
    private final PolicyRepository policyRepository;
    private final PolicyAcknowledgementRepository policyAcknowledgementRepository;

    public static RegulationDto toDto(Regulation row) {
        return RegulationDto.builder()
                .id(row.getId())
                .code(row.getCode())
                .name(row.getName())
                .version(row.getVersion())
                .description(row.getDescription())
                .active(row.getActive())
                .build();
    }

    public static ComplianceRequirementDto toDto(ComplianceRequirement row) {
        return ComplianceRequirementDto.builder()
                .id(row.getId())
                .regulationId(row.getRegulation().getId())
                .regulationCode(row.getRegulation().getCode())
                .requirementCode(row.getRequirementCode())
                .title(row.getTitle())
                .description(row.getDescription())
                .active(row.getActive())
                .build();
    }

    public static RequirementControlMappingDto toDto(RequirementControlMapping row) {
        return RequirementControlMappingDto.builder()
                .id(row.getId())
                .requirementId(row.getRequirement().getId())
                .controlId(row.getControl().getId())
                .controlControlId(row.getControl().getControlId())
                .controlName(row.getControl().getName())
                .coveragePct(row.getCoveragePct())
                .notes(row.getNotes())
                .build();
    }

    public static PolicyRequirementMappingDto toDto(PolicyRequirementMapping row) {
        return PolicyRequirementMappingDto.builder()
                .id(row.getId())
                .policyId(row.getPolicy().getId())
                .policyCode(row.getPolicy().getCode())
                .requirementId(row.getRequirement().getId())
                .requirementCode(row.getRequirement().getRequirementCode())
                .requirementTitle(row.getRequirement().getTitle())
                .notes(row.getNotes())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RegulationDto> listRegulations() {
        return regulationRepository.findAll().stream()
                .sorted(Comparator.comparing(Regulation::getCode))
                .map(ComplianceService::toDto)
                .toList();
    }

    @Transactional
    public RegulationDto createRegulation(String code, String name, String version, String description, Boolean active) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Regulation code is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Regulation name is required");
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (regulationRepository.findByCode(normalizedCode).isPresent()) {
            throw new IllegalArgumentException("Regulation code already exists: " + normalizedCode);
        }
        Regulation row = regulationRepository.save(Regulation.builder()
                .code(normalizedCode)
                .name(name.trim())
                .version(version)
                .description(description)
                .active(active == null ? Boolean.TRUE : active)
                .build());
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<ComplianceRequirementDto> listRequirements(Long regulationId) {
        List<ComplianceRequirement> rows = regulationId == null
                ? complianceRequirementRepository.findAll()
                : complianceRequirementRepository.findByRegulationIdOrderByRequirementCodeAsc(regulationId);
        return rows.stream()
                .sorted(Comparator.comparing(ComplianceRequirement::getRequirementCode))
                .map(ComplianceService::toDto)
                .toList();
    }

    @Transactional
    public ComplianceRequirementDto createRequirement(Long regulationId, String requirementCode, String title, String description, Boolean active) {
        if (regulationId == null) throw new IllegalArgumentException("Regulation id is required");
        if (requirementCode == null || requirementCode.isBlank()) throw new IllegalArgumentException("Requirement code is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Requirement title is required");
        Regulation regulation = regulationRepository.findById(regulationId)
                .orElseThrow(() -> new IllegalArgumentException("Regulation not found: " + regulationId));
        ComplianceRequirement row = complianceRequirementRepository.save(ComplianceRequirement.builder()
                .regulation(regulation)
                .requirementCode(requirementCode.trim())
                .title(title.trim())
                .description(description)
                .active(active == null ? Boolean.TRUE : active)
                .build());
        return toDto(row);
    }

    @Transactional
    public RequirementControlMappingDto mapRequirementToControl(Long requirementId, Long controlId, Integer coveragePct, String notes) {
        ComplianceRequirement requirement = complianceRequirementRepository.findById(requirementId)
                .orElseThrow(() -> new IllegalArgumentException("Requirement not found: " + requirementId));
        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> new IllegalArgumentException("Control not found: " + controlId));
        int normalizedCoverage = coveragePct == null ? 100 : Math.max(0, Math.min(100, coveragePct));
        RequirementControlMapping row = requirementControlMappingRepository.save(RequirementControlMapping.builder()
                .requirement(requirement)
                .control(control)
                .coveragePct(normalizedCoverage)
                .notes(notes)
                .build());
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<RequirementControlMappingDto> listRequirementControlMappings(Long requirementId) {
        List<RequirementControlMapping> rows = requirementId == null
                ? requirementControlMappingRepository.findAll()
                : requirementControlMappingRepository.findByRequirementIdOrderByIdDesc(requirementId);
        return rows.stream()
                .sorted(Comparator.comparing(RequirementControlMapping::getId).reversed())
                .map(ComplianceService::toDto)
                .toList();
    }

    @Transactional
    public PolicyRequirementMappingDto mapPolicyToRequirement(Long policyId, Long requirementId, String notes) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        ComplianceRequirement requirement = complianceRequirementRepository.findById(requirementId)
                .orElseThrow(() -> new IllegalArgumentException("Requirement not found: " + requirementId));
        PolicyRequirementMapping row = policyRequirementMappingRepository.save(PolicyRequirementMapping.builder()
                .policy(policy)
                .requirement(requirement)
                .notes(notes)
                .build());
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<PolicyRequirementMappingDto> listPolicyRequirementMappings(Long policyId) {
        List<PolicyRequirementMapping> rows = policyId == null
                ? policyRequirementMappingRepository.findAll()
                : policyRequirementMappingRepository.findByPolicyIdOrderByIdDesc(policyId);
        return rows.stream()
                .sorted(Comparator.comparing(PolicyRequirementMapping::getId).reversed())
                .map(ComplianceService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplianceKpiDto complianceKpis() {
        long totalPolicies = policyRepository.count();
        long activePolicies = policyRepository.findByStatusInOrderByUpdatedAtDesc(List.of(PolicyStatus.ACTIVE)).size();
        long totalRequirements = complianceRequirementRepository.count();
        long mappedRequirementsToControls = requirementControlMappingRepository.countDistinctRequirementIds();
        long mappedRequirementsToPolicies = policyRequirementMappingRepository.countDistinctRequirementIds();
        long pendingAttestations = policyAcknowledgementRepository.countByStatus(PolicyAcknowledgementStatus.PENDING);
        long overdueAttestations = policyAcknowledgementRepository.countByStatusAndDueAtBefore(PolicyAcknowledgementStatus.PENDING, java.time.Instant.now());
        double controlCoveragePct = pct(mappedRequirementsToControls, totalRequirements);
        double policyCoveragePct = pct(mappedRequirementsToPolicies, totalRequirements);
        return ComplianceKpiDto.builder()
                .totalPolicies(totalPolicies)
                .activePolicies(activePolicies)
                .totalRequirements(totalRequirements)
                .mappedRequirementsToControls(mappedRequirementsToControls)
                .mappedRequirementsToPolicies(mappedRequirementsToPolicies)
                .controlCoveragePct(controlCoveragePct)
                .policyCoveragePct(policyCoveragePct)
                .pendingAttestations(pendingAttestations)
                .overdueAttestations(overdueAttestations)
                .build();
    }

    private double pct(long value, long total) {
        if (total <= 0) return 0.0;
        return BigDecimal.valueOf(value * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
