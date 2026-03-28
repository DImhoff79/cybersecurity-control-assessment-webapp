package com.cyberassessment.service;

import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.dto.QuestionLibraryRowDto;
import com.cyberassessment.dto.QuestionMappingRefDto;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.Question;
import com.cyberassessment.entity.QuestionControlId;
import com.cyberassessment.entity.QuestionControlMapping;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.repository.QuestionControlMappingRepository;
import com.cyberassessment.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ControlRepository controlRepository;
    private final QuestionControlMappingRepository questionControlMappingRepository;

    @Transactional(readOnly = true)
    public List<QuestionDto> findByControlId(Long controlId) {
        return questionControlMappingRepository.findByControl_IdOrderByQuestionDisplayOrderAsc(controlId).stream()
                .map(m -> ControlService.questionToDto(m.getQuestion(), controlId, m))
                .collect(Collectors.toList());
    }

    @Transactional
    public QuestionDto create(Long controlId, String questionText, Integer displayOrder, String helpText, Boolean askOwner) {
        Control control = controlRepository.findById(controlId).orElseThrow(() -> new IllegalArgumentException("Control not found: " + controlId));
        String normalizedQuestionText = questionText != null ? questionText.trim() : null;
        if (normalizedQuestionText == null || normalizedQuestionText.isBlank()) {
            throw new IllegalArgumentException("Question text is required");
        }

        Question existing = questionRepository.findFirstByQuestionTextIgnoreCase(normalizedQuestionText).orElse(null);
        if (existing != null) {
            if (!questionControlMappingRepository.existsByControl_IdAndQuestion_Id(controlId, existing.getId())) {
                questionControlMappingRepository.save(QuestionControlMapping.builder()
                        .id(new QuestionControlId(existing.getId(), controlId))
                        .question(existing)
                        .control(control)
                        .build());
            }
            QuestionControlMapping mapping = questionControlMappingRepository.findById(new QuestionControlId(existing.getId(), controlId)).orElse(null);
            return ControlService.questionToDto(existing, controlId, mapping);
        }

        if (displayOrder == null) {
            long count = questionControlMappingRepository.countByControl_Id(controlId);
            displayOrder = (int) count;
        }
        Question q = Question.builder()
                .control(control)
                .questionText(normalizedQuestionText)
                .displayOrder(displayOrder)
                .helpText(helpText)
                .askOwner(askOwner != null ? askOwner : true)
                .build();
        q = questionRepository.save(q);
        QuestionControlMapping mapping = questionControlMappingRepository.save(QuestionControlMapping.builder()
                .id(new QuestionControlId(q.getId(), controlId))
                .question(q)
                .control(control)
                .build());
        return ControlService.questionToDto(q, controlId, mapping);
    }

    @Transactional
    public QuestionDto update(Long controlId, Long id, String questionText, Integer displayOrder, String helpText, Boolean askOwner) {
        if (!questionControlMappingRepository.existsByControl_IdAndQuestion_Id(controlId, id)) {
            throw new IllegalArgumentException("Question not mapped to this control");
        }
        Question q = questionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
        if (questionText != null) q.setQuestionText(questionText);
        if (displayOrder != null) q.setDisplayOrder(displayOrder);
        if (helpText != null) q.setHelpText(helpText);
        if (askOwner != null) q.setAskOwner(askOwner);
        q = questionRepository.save(q);
        QuestionControlMapping mapping = questionControlMappingRepository.findById(new QuestionControlId(id, controlId)).orElse(null);
        return ControlService.questionToDto(q, controlId, mapping);
    }

    @Transactional
    public QuestionDto updateMapping(Long controlId, Long id, String mappingRationale, java.math.BigDecimal mappingWeight,
                                     java.time.Instant effectiveFrom, java.time.Instant effectiveTo) {
        QuestionControlMapping mapping = questionControlMappingRepository.findById(new QuestionControlId(id, controlId))
                .orElseThrow(() -> new IllegalArgumentException("Question mapping not found"));
        if (mappingRationale != null) mapping.setMappingRationale(mappingRationale);
        if (mappingWeight != null) mapping.setMappingWeight(mappingWeight);
        if (effectiveFrom != null) mapping.setEffectiveFrom(effectiveFrom);
        if (effectiveTo != null) mapping.setEffectiveTo(effectiveTo);
        mapping = questionControlMappingRepository.save(mapping);
        return ControlService.questionToDto(mapping.getQuestion(), controlId, mapping);
    }

    @Transactional
    public void delete(Long controlId, Long id) {
        Question question = questionRepository.findById(id).orElse(null);
        if (question == null) {
            throw new IllegalArgumentException("Question not found: " + id);
        }
        if (!questionControlMappingRepository.existsByControl_IdAndQuestion_Id(controlId, id)) {
            throw new IllegalArgumentException("Question not mapped to this control");
        }
        long countForControl = questionControlMappingRepository.countByControl_Id(controlId);
        if (countForControl <= 1) {
            throw new IllegalArgumentException("Each control must keep at least one plain-language question");
        }
        questionControlMappingRepository.deleteById(new QuestionControlId(id, controlId));
        long remainingMappings = questionControlMappingRepository.countByQuestion_Id(id);
        if (remainingMappings == 0) {
            questionRepository.delete(question);
        }
    }

    @Transactional(readOnly = true)
    public List<QuestionLibraryRowDto> listLibrary() {
        return questionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toLibraryRow)
                .collect(Collectors.toList());
    }

    private QuestionLibraryRowDto toLibraryRow(Question q) {
        List<QuestionMappingRefDto> refs = questionControlMappingRepository.findByQuestion_Id(q.getId()).stream()
                .map(this::toMappingRef)
                .collect(Collectors.toList());
        return QuestionLibraryRowDto.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .helpText(q.getHelpText())
                .askOwner(q.getAskOwner())
                .displayOrder(q.getDisplayOrder())
                .mappings(refs)
                .build();
    }

    private QuestionMappingRefDto toMappingRef(QuestionControlMapping m) {
        Control c = m.getControl();
        return QuestionMappingRefDto.builder()
                .controlDbId(c.getId())
                .controlId(c.getControlId())
                .name(c.getName())
                .description(c.getDescription())
                .framework(c.getFramework())
                .mappingRationale(m.getMappingRationale())
                .mappingWeight(m.getMappingWeight())
                .effectiveFrom(m.getEffectiveFrom())
                .effectiveTo(m.getEffectiveTo())
                .build();
    }

    @Transactional
    public QuestionDto createUnmapped(String questionText, Integer displayOrder, String helpText, Boolean askOwner) {
        String normalized = questionText != null ? questionText.trim() : "";
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Question text is required");
        }
        if (questionRepository.findFirstByQuestionTextIgnoreCase(normalized).isPresent()) {
            throw new IllegalArgumentException("A question with this text already exists");
        }
        int order = displayOrder != null ? displayOrder : 0;
        Question q = Question.builder()
                .control(null)
                .questionText(normalized)
                .displayOrder(order)
                .helpText(helpText)
                .askOwner(askOwner != null ? askOwner : true)
                .build();
        q = questionRepository.save(q);
        return ControlService.questionToDto(q);
    }

    @Transactional
    public void deleteUnmapped(Long id) {
        Question q = questionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        if (questionControlMappingRepository.countByQuestion_Id(id) > 0) {
            throw new IllegalArgumentException("Remove all control links before deleting this question");
        }
        questionRepository.delete(q);
    }

    /** Link an existing library question to a control (e.g. drag-and-drop in mapping studio). */
    @Transactional
    public QuestionDto linkQuestionToControl(Long controlId, Long questionId, Integer displayOrder) {
        Control control = controlRepository.findById(controlId).orElseThrow(() -> new IllegalArgumentException("Control not found"));
        Question q = questionRepository.findById(questionId).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        if (questionControlMappingRepository.existsByControl_IdAndQuestion_Id(controlId, questionId)) {
            throw new IllegalArgumentException("Question already linked to this control");
        }
        int order = displayOrder != null ? displayOrder : (int) questionControlMappingRepository.countByControl_Id(controlId);
        q.setDisplayOrder(order);
        questionRepository.save(q);
        QuestionControlMapping mapping = questionControlMappingRepository.save(QuestionControlMapping.builder()
                .id(new QuestionControlId(q.getId(), controlId))
                .question(q)
                .control(control)
                .build());
        return ControlService.questionToDto(q, controlId, mapping);
    }
}
