package com.cyberassessment.service;

import com.cyberassessment.dto.IntakeWizardStepDto;
import com.cyberassessment.dto.branching.BranchingNodeDto;
import com.cyberassessment.dto.branching.BranchingWorkflowGraphDto;
import com.cyberassessment.entity.Question;
import com.cyberassessment.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IntakeWizardService {

    private final QuestionRepository questionRepository;
    private final CanonicalIntakeFlowService canonicalIntakeFlowService;

    @Transactional(readOnly = true)
    public List<IntakeWizardStepDto> loadWizardSteps() {
        Optional<BranchingWorkflowGraphDto> graphOpt = canonicalIntakeFlowService.loadGraph();
        List<Question> allIntake = questionRepository.findByIntakeStepKeyIsNotNullOrderByDisplayOrderAsc();
        if (graphOpt.isEmpty()) {
            return allIntake.stream().map(this::toStep).toList();
        }
        BranchingWorkflowGraphDto graph = graphOpt.get();
        List<BranchingNodeDto> ordered = graph.getNodes() == null ? List.of() : graph.getNodes().stream()
                .filter(n -> n.getStableKey() != null && !n.getStableKey().isBlank())
                .filter(n -> n.getQuestionType() == null || !"END".equalsIgnoreCase(n.getQuestionType()))
                .sorted(Comparator.comparingInt(n -> n.getPosX() != null ? n.getPosX() : 0))
                .toList();
        Set<String> seen = new LinkedHashSet<>();
        List<IntakeWizardStepDto> out = new ArrayList<>();
        for (BranchingNodeDto n : ordered) {
            String key = n.getStableKey().trim();
            questionRepository.findByIntakeStepKey(key).ifPresent(q -> {
                if (seen.add(key)) {
                    out.add(toStep(q));
                }
            });
        }
        for (Question q : allIntake) {
            String k = q.getIntakeStepKey();
            if (k != null && seen.add(k.trim())) {
                out.add(toStep(q));
            }
        }
        return out;
    }

    private IntakeWizardStepDto toStep(Question q) {
        return IntakeWizardStepDto.builder()
                .questionId(q.getId())
                .stepKey(q.getIntakeStepKey())
                .inputType(q.getIntakeInputType())
                .questionText(q.getQuestionText())
                .helpText(q.getHelpText())
                .choicesJson(q.getIntakeChoicesJson())
                .displayOrder(q.getDisplayOrder())
                .build();
    }
}
