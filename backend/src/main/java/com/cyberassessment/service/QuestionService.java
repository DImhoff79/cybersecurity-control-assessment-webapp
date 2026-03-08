package com.cyberassessment.service;

import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.Question;
import com.cyberassessment.entity.QuestionControlId;
import com.cyberassessment.entity.QuestionControlMapping;
import com.cyberassessment.repository.ControlRepository;
import com.cyberassessment.repository.QuestionControlMappingRepository;
import com.cyberassessment.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
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
}
