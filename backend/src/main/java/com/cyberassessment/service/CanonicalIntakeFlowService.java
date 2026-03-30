package com.cyberassessment.service;

import com.cyberassessment.dto.branching.BranchingWorkflowGraphDto;
import com.cyberassessment.entity.CanonicalIntakeFlow;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.CanonicalIntakeFlowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CanonicalIntakeFlowService {

    private final CanonicalIntakeFlowRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<BranchingWorkflowGraphDto> loadGraph() {
        return repository.findById(CanonicalIntakeFlow.SINGLETON_ID).map(row -> {
            try {
                String json = row.getGraphJson();
                if (json == null || json.isBlank() || "{}".equals(json.trim())) {
                    return null;
                }
                return objectMapper.readValue(json, BranchingWorkflowGraphDto.class);
            } catch (Exception e) {
                return null;
            }
        }).filter(g -> g != null && g.getNodes() != null && !g.getNodes().isEmpty());
    }

    @Transactional
    public void saveGraph(BranchingWorkflowGraphDto graph, User updatedBy) {
        try {
            String json = objectMapper.writeValueAsString(graph);
            CanonicalIntakeFlow row = repository.findById(CanonicalIntakeFlow.SINGLETON_ID)
                    .orElse(CanonicalIntakeFlow.builder().id(CanonicalIntakeFlow.SINGLETON_ID).build());
            row.setGraphJson(json);
            row.setUpdatedAt(Instant.now());
            row.setUpdatedBy(updatedBy);
            repository.save(row);
        } catch (Exception e) {
            throw new IllegalStateException("Could not persist canonical intake flow", e);
        }
    }
}
