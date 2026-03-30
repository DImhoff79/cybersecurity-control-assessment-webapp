package com.cyberassessment.service;

import com.cyberassessment.dto.ControlDto;
import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.ControlFramework;
import com.cyberassessment.entity.Question;
import com.cyberassessment.entity.RegulatoryScopeTag;
import com.cyberassessment.model.ControlResponderAudience;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.repository.QuestionControlMappingRepository;
import com.cyberassessment.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ControlService {

    private final ControlRepository controlRepository;
    private final QuestionControlMappingRepository questionControlMappingRepository;
    private final QuestionRepository questionRepository;

    public static ControlDto toDto(Control c) {
        if (c == null) return null;
        List<String> scopes = new ArrayList<>();
        if (c.getRegulatoryScopes() != null && !c.getRegulatoryScopes().isEmpty()) {
            scopes = c.getRegulatoryScopes().stream().map(RegulatoryScopeTag::name).sorted().collect(Collectors.toList());
        }
        return ControlDto.builder()
                .id(c.getId())
                .controlId(c.getControlId())
                .name(c.getName())
                .description(c.getDescription())
                .framework(c.getFramework())
                .enabled(c.getEnabled())
                .category(c.getCategory())
                .regulatoryScopes(scopes)
                .build();
    }

    private Map<Long, ControlResponderAudience> loadResponderAudiences(List<Long> controlIds) {
        if (controlIds == null || controlIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = questionRepository.summarizeAskOwnerByControlIds(controlIds);
        Map<Long, ControlResponderAudience> out = new HashMap<>();
        for (Object[] row : rows) {
            Long cid = (Long) row[0];
            Number trueSum = (Number) row[1];
            Number total = (Number) row[2];
            long t = trueSum != null ? trueSum.longValue() : 0;
            long tot = total != null ? total.longValue() : 0;
            out.put(cid, ControlResponderAudience.fromAskOwnerCounts(t, tot));
        }
        for (Long id : controlIds) {
            out.putIfAbsent(id, ControlResponderAudience.APPLICATION_OWNER);
        }
        return out;
    }

    private void applyResponderAudience(ControlDto dto, Map<Long, ControlResponderAudience> audiences) {
        if (dto == null || audiences == null) return;
        dto.setResponderAudience(audiences.getOrDefault(dto.getId(), ControlResponderAudience.APPLICATION_OWNER));
    }

    public static QuestionDto questionToDto(Question q) {
        if (q.getControl() == null) {
            return questionToDto(q, null, null);
        }
        return questionToDto(q, q.getControl().getId(), null);
    }

    public static QuestionDto questionToDto(Question q, Long controlId) {
        return questionToDto(q, controlId, null);
    }

    public static QuestionDto questionToDto(Question q, Long controlId, com.cyberassessment.entity.QuestionControlMapping mapping) {
        if (q == null) return null;
        return QuestionDto.builder()
                .id(q.getId())
                .controlId(controlId)
                .questionText(q.getQuestionText())
                .displayOrder(q.getDisplayOrder())
                .helpText(q.getHelpText())
                .askOwner(q.getAskOwner())
                .mappingRationale(mapping != null ? mapping.getMappingRationale() : null)
                .mappingWeight(mapping != null ? mapping.getMappingWeight() : null)
                .effectiveFrom(mapping != null ? mapping.getEffectiveFrom() : null)
                .effectiveTo(mapping != null ? mapping.getEffectiveTo() : null)
                .intakeStepKey(q.getIntakeStepKey())
                .intakeInputType(q.getIntakeInputType())
                .intakeChoicesJson(q.getIntakeChoicesJson())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ControlDto> findAll(ControlFramework framework, Boolean enabled, boolean includeQuestions) {
        List<Control> list;
        if (framework != null && enabled != null) {
            list = controlRepository.findByFrameworkAndEnabled(framework, enabled);
        } else if (framework != null) {
            list = controlRepository.findByFramework(framework);
        } else if (enabled != null) {
            list = controlRepository.findByEnabled(enabled);
        } else {
            list = controlRepository.findAll();
        }
        List<Long> ids = list.stream().map(Control::getId).toList();
        Map<Long, ControlResponderAudience> audiences = loadResponderAudiences(ids);
        return list.stream().map(c -> {
            ControlDto dto = toDto(c);
            applyResponderAudience(dto, audiences);
            if (includeQuestions) {
                dto.setQuestions(questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(c.getId()).stream()
                        .map(m -> questionToDto(m.getQuestion(), c.getId(), m))
                        .collect(Collectors.toList()));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ControlDto findById(Long id, boolean includeQuestions) {
        return controlRepository.findById(id).map(c -> {
            ControlDto dto = toDto(c);
            applyResponderAudience(dto, loadResponderAudiences(List.of(id)));
            if (includeQuestions) {
                dto.setQuestions(questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(c.getId()).stream()
                        .map(m -> questionToDto(m.getQuestion(), c.getId(), m))
                        .collect(Collectors.toList()));
            }
            return dto;
        }).orElse(null);
    }

    @Transactional
    public ControlDto update(Long id, String name, String description, Boolean enabled, List<String> regulatoryScopes) {
        Control c = controlRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Control not found: " + id));
        if (name != null) c.setName(name);
        if (description != null) c.setDescription(description);
        if (enabled != null) c.setEnabled(enabled);
        if (regulatoryScopes != null) {
            applyRegulatoryScopes(c, regulatoryScopes);
        }
        c = controlRepository.save(c);
        ControlDto dto = toDto(c);
        applyResponderAudience(dto, loadResponderAudiences(List.of(id)));
        return dto;
    }

    private static void applyRegulatoryScopes(Control c, List<String> regulatoryScopes) {
        c.getRegulatoryScopes().clear();
        for (String s : regulatoryScopes) {
            if (s == null || s.isBlank()) {
                continue;
            }
            c.getRegulatoryScopes().add(RegulatoryScopeTag.valueOf(s.trim().toUpperCase()));
        }
    }

    @Transactional
    public ControlDto patch(Long id, String name, String description, Boolean enabled, List<String> regulatoryScopes) {
        return update(id, name, description, enabled, regulatoryScopes);
    }

    @Transactional
    public ControlDto create(String controlId, String name, String description, ControlFramework framework, Boolean enabled, String category) {
        if (controlId == null || controlId.isBlank()) throw new IllegalArgumentException("controlId is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (framework == null) throw new IllegalArgumentException("framework is required");
        String normalizedControlId = controlId.trim();
        if (controlRepository.existsByControlIdAndFramework(normalizedControlId, framework)) {
            throw new IllegalArgumentException("Control already exists for framework");
        }
        Control control = Control.builder()
                .controlId(normalizedControlId)
                .name(name.trim())
                .description(description)
                .framework(framework)
                .enabled(enabled != null ? enabled : true)
                .category(category)
                .build();
        Control saved = controlRepository.save(control);
        ControlDto dto = toDto(saved);
        applyResponderAudience(dto, loadResponderAudiences(List.of(saved.getId())));
        return dto;
    }

    @Transactional
    public void delete(Long id) {
        Control control = controlRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Control not found: " + id));
        var mappings = questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(id);
        questionControlMappingRepository.deleteAll(mappings);
        for (var mapping : mappings) {
            Long questionId = mapping.getQuestion().getId();
            if (questionControlMappingRepository.countByQuestion_Id(questionId) == 0) {
                questionRepository.deleteById(questionId);
            }
        }
        controlRepository.delete(control);
    }
}
