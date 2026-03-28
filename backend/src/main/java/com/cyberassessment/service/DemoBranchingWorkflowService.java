package com.cyberassessment.service;

import com.cyberassessment.dto.branching.*;
import com.cyberassessment.entity.DemoBranchingEdge;
import com.cyberassessment.entity.DemoBranchingNode;
import com.cyberassessment.entity.DemoBranchingWorkflow;
import com.cyberassessment.entity.DemoBranchingWorkflowVersion;
import com.cyberassessment.repository.DemoBranchingEdgeRepository;
import com.cyberassessment.repository.DemoBranchingNodeRepository;
import com.cyberassessment.repository.DemoBranchingWorkflowRepository;
import com.cyberassessment.repository.DemoBranchingWorkflowVersionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DemoBranchingWorkflowService {

    private final DemoBranchingWorkflowRepository workflowRepository;
    private final DemoBranchingWorkflowVersionRepository versionRepository;
    private final DemoBranchingNodeRepository nodeRepository;
    private final DemoBranchingEdgeRepository edgeRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BranchingWorkflowGraphDto getGraph(Long versionId) {
        DemoBranchingWorkflowVersion v = resolveVersion(versionId);
        return toGraphDto(v);
    }

    private DemoBranchingWorkflowVersion resolveVersion(Long versionId) {
        if (versionId != null) {
            return versionRepository.findById(versionId).orElseThrow(() -> new IllegalArgumentException("Unknown version"));
        }
        DemoBranchingWorkflow wf = workflowRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No demo workflow"));
        return versionRepository
                .findFirstByWorkflow_IdOrderByIdDesc(wf.getId())
                .orElseThrow(() -> new IllegalStateException("No demo workflow version"));
    }

    private BranchingWorkflowGraphDto toGraphDto(DemoBranchingWorkflowVersion v) {
        DemoBranchingWorkflow wf = v.getWorkflow();
        List<DemoBranchingNode> nodes = nodeRepository.findByVersion_Id(v.getId());
        List<DemoBranchingEdge> edges = edgeRepository.findByVersion_IdWithEndpoints(v.getId());
        List<BranchingNodeDto> nodeDtos = nodes.stream().map(this::toNodeDto).toList();
        List<BranchingEdgeDto> edgeDtos = edges.stream()
                .map(e -> BranchingEdgeDto.builder()
                        .id(e.getId())
                        .fromNodeId(e.getFromNode().getId())
                        .toNodeId(e.getToNode().getId())
                        .sortOrder(e.getSortOrder())
                        .conditionKind(e.getConditionKind())
                        .conditionValue(e.getConditionValue())
                        .build())
                .toList();
        return BranchingWorkflowGraphDto.builder()
                .workflowId(wf.getId())
                .workflowName(wf.getName())
                .workflowDescription(wf.getDescription())
                .versionId(v.getId())
                .versionLabel(v.getVersionLabel())
                .startNodeId(v.getStartNodeId())
                .nodes(nodeDtos)
                .edges(edgeDtos)
                .build();
    }

    private BranchingNodeDto toNodeDto(DemoBranchingNode n) {
        return BranchingNodeDto.builder()
                .id(n.getId())
                .stableKey(n.getStableKey())
                .title(n.getTitle())
                .body(n.getBody())
                .questionType(n.getQuestionType())
                .choices(parseChoices(n.getChoicesJson()))
                .posX(n.getPosX())
                .posY(n.getPosY())
                .build();
    }

    private List<BranchingChoiceDto> parseChoices(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, String>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            List<BranchingChoiceDto> out = new ArrayList<>();
            for (Map<String, String> m : raw) {
                String id = m.get("id");
                String label = Optional.ofNullable(m.get("label")).orElse(id);
                if (id != null) {
                    out.add(new BranchingChoiceDto(id, label));
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public BranchingResolveResponse resolve(BranchingResolveRequest req) {
        if (req.getVersionId() == null || req.getFromNodeId() == null) {
            throw new IllegalArgumentException("versionId and fromNodeId are required");
        }
        DemoBranchingNode from = nodeRepository
                .findById(req.getFromNodeId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown node"));
        if (!from.getVersion().getId().equals(req.getVersionId())) {
            throw new IllegalArgumentException("Node does not belong to this version");
        }
        if ("END".equalsIgnoreCase(from.getQuestionType())) {
            return BranchingResolveResponse.builder().finished(true).nextNode(null).build();
        }
        List<DemoBranchingEdge> edges = edgeRepository.findByFromNodeIdOrderBySortOrderAsc(from.getId());
        String val = req.getValue() == null ? "" : req.getValue().trim();
        DemoBranchingEdge picked = null;
        for (DemoBranchingEdge e : edges) {
            if (matchesEdge(e, val)) {
                picked = e;
                break;
            }
        }
        if (picked == null) {
            throw new IllegalArgumentException("No transition matches this answer for the current step");
        }
        DemoBranchingNode next = picked.getToNode();
        BranchingNodeDto nextDto = toNodeDto(next);
        boolean finished = "END".equalsIgnoreCase(next.getQuestionType());
        return BranchingResolveResponse.builder().nextNode(nextDto).finished(finished).build();
    }

    private boolean matchesEdge(DemoBranchingEdge e, String value) {
        String kind = e.getConditionKind() == null ? "" : e.getConditionKind().toUpperCase();
        return switch (kind) {
            case "ALWAYS" -> true;
            case "YES" -> "yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
            case "NO" -> "no".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
            case "OPTION" -> e.getConditionValue() != null && e.getConditionValue().equals(value);
            default -> false;
        };
    }

    @Transactional
    public BranchingWorkflowGraphDto saveGraph(BranchingWorkflowSaveDto dto) {
        if (dto.getVersionId() == null) {
            throw new IllegalArgumentException("versionId is required");
        }
        if (dto.getNodes() == null || dto.getNodes().isEmpty()) {
            throw new IllegalArgumentException("At least one node is required");
        }
        if (dto.getStartStableKey() == null || dto.getStartStableKey().isBlank()) {
            throw new IllegalArgumentException("startStableKey is required");
        }
        DemoBranchingWorkflowVersion v = versionRepository
                .findById(dto.getVersionId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown version"));

        v.getEdges().clear();
        v.getNodes().clear();
        versionRepository.saveAndFlush(v);

        Map<String, DemoBranchingNode> byKey = new LinkedHashMap<>();
        for (BranchingNodeSaveDto n : dto.getNodes()) {
            if (n.getStableKey() == null || n.getStableKey().isBlank()) {
                throw new IllegalArgumentException("Each node needs a stableKey");
            }
            String key = n.getStableKey().trim();
            if (byKey.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate stableKey: " + key);
            }
            String qType = n.getQuestionType() == null ? "TEXT" : n.getQuestionType().trim();
            DemoBranchingNode node = DemoBranchingNode.builder()
                    .version(v)
                    .stableKey(key)
                    .title(n.getTitle() != null ? n.getTitle() : "")
                    .body(n.getBody())
                    .questionType(qType)
                    .choicesJson(n.getChoicesJson())
                    .posX(n.getPosX() != null ? n.getPosX() : 0)
                    .posY(n.getPosY() != null ? n.getPosY() : 0)
                    .build();
            v.getNodes().add(node);
            byKey.put(key, node);
        }
        versionRepository.saveAndFlush(v);

        if (dto.getEdges() != null) {
            for (BranchingEdgeSaveDto e : dto.getEdges()) {
                DemoBranchingNode from = byKey.get(e.getFromStableKey());
                DemoBranchingNode to = byKey.get(e.getToStableKey());
                if (from == null || to == null) {
                    throw new IllegalArgumentException("Edge references unknown stableKey");
                }
                String ck = e.getConditionKind() == null ? "ALWAYS" : e.getConditionKind().trim();
                DemoBranchingEdge edge = DemoBranchingEdge.builder()
                        .version(v)
                        .fromNode(from)
                        .toNode(to)
                        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
                        .conditionKind(ck)
                        .conditionValue(e.getConditionValue())
                        .build();
                v.getEdges().add(edge);
            }
        }
        DemoBranchingNode start = byKey.get(dto.getStartStableKey().trim());
        if (start == null) {
            throw new IllegalArgumentException("startStableKey does not match any node");
        }
        v.setStartNodeId(start.getId());
        versionRepository.save(v);
        return toGraphDto(versionRepository.findById(v.getId()).orElseThrow());
    }
}
