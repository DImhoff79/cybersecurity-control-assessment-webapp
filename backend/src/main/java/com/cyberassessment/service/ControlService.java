package com.cyberassessment.service;

import com.cyberassessment.dto.ControlDto;
import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.ControlFramework;
import com.cyberassessment.entity.Question;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.repository.QuestionControlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ControlService {

    private final ControlRepository controlRepository;
    private final QuestionControlMappingRepository questionControlMappingRepository;

    public static ControlDto toDto(Control c) {
        if (c == null) return null;
        return ControlDto.builder()
                .id(c.getId())
                .controlId(c.getControlId())
                .name(c.getName())
                .description(c.getDescription())
                .framework(c.getFramework())
                .enabled(c.getEnabled())
                .category(c.getCategory())
                .build();
    }

    public static QuestionDto questionToDto(Question q) {
        return questionToDto(q, q.getControl().getId());
    }

    public static QuestionDto questionToDto(Question q, Long controlId) {
        if (q == null) return null;
        return QuestionDto.builder()
                .id(q.getId())
                .controlId(controlId)
                .questionText(q.getQuestionText())
                .displayOrder(q.getDisplayOrder())
                .helpText(q.getHelpText())
                .askOwner(q.getAskOwner())
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
        return list.stream().map(c -> {
            ControlDto dto = toDto(c);
            if (includeQuestions) {
                dto.setQuestions(questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(c.getId()).stream()
                        .map(m -> questionToDto(m.getQuestion(), c.getId()))
                        .collect(Collectors.toList()));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ControlDto findById(Long id, boolean includeQuestions) {
        return controlRepository.findById(id).map(c -> {
            ControlDto dto = toDto(c);
            if (includeQuestions) {
                dto.setQuestions(questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(c.getId()).stream()
                        .map(m -> questionToDto(m.getQuestion(), c.getId()))
                        .collect(Collectors.toList()));
            }
            return dto;
        }).orElse(null);
    }

    @Transactional
    public ControlDto update(Long id, String name, String description, Boolean enabled) {
        Control c = controlRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Control not found: " + id));
        if (name != null) c.setName(name);
        if (description != null) c.setDescription(description);
        if (enabled != null) c.setEnabled(enabled);
        c = controlRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public ControlDto patch(Long id, String name, String description, Boolean enabled) {
        return update(id, name, description, enabled);
    }
}
