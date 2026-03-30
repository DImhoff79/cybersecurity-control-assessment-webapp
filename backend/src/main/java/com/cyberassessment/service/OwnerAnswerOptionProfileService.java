package com.cyberassessment.service;

import com.cyberassessment.dto.OwnerAnswerOptionEntryDto;
import com.cyberassessment.dto.OwnerAnswerOptionProfileDto;
import com.cyberassessment.dto.ResolvedOwnerAnswerUi;
import com.cyberassessment.entity.OwnerAnswerOptionProfile;
import com.cyberassessment.entity.Question;
import com.cyberassessment.repository.OwnerAnswerOptionProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerAnswerOptionProfileService {

    private static final Set<String> REQUIRED_VALUES = Set.of(
            "UNANSWERED", "YES", "PARTIAL", "NO", "NOT_APPLICABLE"
    );

    private final OwnerAnswerOptionProfileRepository ownerAnswerOptionProfileRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public OwnerAnswerOptionProfile getDefaultProfile() {
        return ownerAnswerOptionProfileRepository.findByCode(OwnerAnswerOptionProfile.DEFAULT_CODE)
                .orElseThrow(() -> new IllegalStateException("Missing owner answer option profile: " + OwnerAnswerOptionProfile.DEFAULT_CODE));
    }

    @Transactional(readOnly = true)
    public ResolvedOwnerAnswerUi resolveForQuestion(Question q) {
        OwnerAnswerOptionProfile p = q.getOwnerAnswerOptionProfile();
        if (p == null) {
            p = getDefaultProfile();
        }
        return ResolvedOwnerAnswerUi.builder()
                .profileId(p.getId())
                .code(p.getCode())
                .fieldLabel(p.getFieldLabel())
                .fieldHint(p.getFieldHint())
                .options(parseOptions(p.getOptionsJson()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<OwnerAnswerOptionProfileDto> listAll() {
        return ownerAnswerOptionProfileRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OwnerAnswerOptionProfileDto getById(Long id) {
        return ownerAnswerOptionProfileRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Owner answer option profile not found: " + id));
    }

    @Transactional
    public OwnerAnswerOptionProfileDto update(Long id, OwnerAnswerOptionProfileDto request) {
        OwnerAnswerOptionProfile p = ownerAnswerOptionProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Owner answer option profile not found: " + id));
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            p.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getFieldLabel() != null) {
            p.setFieldLabel(request.getFieldLabel().isBlank() ? null : request.getFieldLabel().trim());
        }
        if (request.getFieldHint() != null) {
            p.setFieldHint(request.getFieldHint().isBlank() ? null : request.getFieldHint().trim());
        }
        if (request.getOptionsJson() != null) {
            validateAndNormalizeOptionsJson(request.getOptionsJson());
            p.setOptionsJson(request.getOptionsJson().trim());
        }
        p = ownerAnswerOptionProfileRepository.save(p);
        return toDto(p);
    }

    public List<OwnerAnswerOptionEntryDto> parseOptions(String optionsJson) {
        try {
            List<OwnerAnswerOptionEntryDto> list = objectMapper.readValue(
                    optionsJson,
                    new TypeReference<>() {
                    });
            if (list == null) {
                return List.of();
            }
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(e -> OwnerAnswerOptionEntryDto.builder()
                            .value(e.getValue() != null ? e.getValue().trim() : null)
                            .label(e.getLabel() != null ? e.getLabel().trim() : null)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid options_json: " + e.getMessage());
        }
    }

    public void validateAndNormalizeOptionsJson(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            throw new IllegalArgumentException("options_json is required");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(optionsJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("options_json must be valid JSON");
        }
        if (!root.isArray()) {
            throw new IllegalArgumentException("options_json must be a JSON array");
        }
        Set<String> seen = new HashSet<>();
        Set<String> values = new HashSet<>();
        for (JsonNode node : root) {
            if (!node.isObject()) {
                throw new IllegalArgumentException("Each options_json entry must be an object");
            }
            JsonNode v = node.get("value");
            JsonNode l = node.get("label");
            if (v == null || !v.isTextual() || v.asText().isBlank()) {
                throw new IllegalArgumentException("Each option needs a non-blank value");
            }
            if (l == null || !l.isTextual() || l.asText().isBlank()) {
                throw new IllegalArgumentException("Each option needs a non-blank label");
            }
            String value = v.asText().trim();
            if (!seen.add(value)) {
                throw new IllegalArgumentException("Duplicate option value: " + value);
            }
            values.add(value);
        }
        if (!values.containsAll(REQUIRED_VALUES)) {
            throw new IllegalArgumentException(
                    "options_json must include values: " + String.join(", ", REQUIRED_VALUES));
        }
    }

    private OwnerAnswerOptionProfileDto toDto(OwnerAnswerOptionProfile p) {
        return OwnerAnswerOptionProfileDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .displayName(p.getDisplayName())
                .fieldLabel(p.getFieldLabel())
                .fieldHint(p.getFieldHint())
                .optionsJson(p.getOptionsJson())
                .build();
    }
}
