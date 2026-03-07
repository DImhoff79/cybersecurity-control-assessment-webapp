package com.cyberassessment.service;

import com.cyberassessment.dto.QuestionDto;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.Question;
import com.cyberassessment.repository.ControlRepository;
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

    @Transactional(readOnly = true)
    public List<QuestionDto> findByControlId(Long controlId) {
        return questionRepository.findByControlIdOrderByDisplayOrderAsc(controlId).stream()
                .map(ControlService::questionToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuestionDto create(Long controlId, String questionText, Integer displayOrder, String helpText) {
        Control control = controlRepository.findById(controlId).orElseThrow(() -> new IllegalArgumentException("Control not found: " + controlId));
        if (displayOrder == null) {
            long count = questionRepository.findByControlIdOrderByDisplayOrderAsc(controlId).size();
            displayOrder = (int) count;
        }
        Question q = Question.builder()
                .control(control)
                .questionText(questionText)
                .displayOrder(displayOrder)
                .helpText(helpText)
                .build();
        q = questionRepository.save(q);
        return ControlService.questionToDto(q);
    }

    @Transactional
    public QuestionDto update(Long id, String questionText, Integer displayOrder, String helpText) {
        Question q = questionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
        if (questionText != null) q.setQuestionText(questionText);
        if (displayOrder != null) q.setDisplayOrder(displayOrder);
        if (helpText != null) q.setHelpText(helpText);
        q = questionRepository.save(q);
        return ControlService.questionToDto(q);
    }

    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new IllegalArgumentException("Question not found: " + id);
        }
        questionRepository.deleteById(id);
    }
}
