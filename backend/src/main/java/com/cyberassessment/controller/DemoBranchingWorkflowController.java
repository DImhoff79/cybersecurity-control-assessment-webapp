package com.cyberassessment.controller;

import com.cyberassessment.dto.branching.BranchingResolveRequest;
import com.cyberassessment.dto.branching.BranchingWorkflowGraphDto;
import com.cyberassessment.dto.branching.BranchingWorkflowSaveDto;
import com.cyberassessment.service.DemoBranchingWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demo/branching-workflows")
@RequiredArgsConstructor
public class DemoBranchingWorkflowController {

    private final DemoBranchingWorkflowService demoBranchingWorkflowService;

    @GetMapping("/graph")
    public ResponseEntity<?> getGraph(@RequestParam(required = false) Long versionId) {
        try {
            return ResponseEntity.ok(demoBranchingWorkflowService.getGraph(versionId));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            Map.of(
                                    "message",
                                    ex.getMessage(),
                                    "detail",
                                    "Apply Flyway migrations through V36 and restart the backend."));
        }
    }

    @PostMapping("/resolve")
    public ResponseEntity<?> resolve(@RequestBody BranchingResolveRequest body) {
        try {
            return ResponseEntity.ok(demoBranchingWorkflowService.resolve(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/graph")
    @PreAuthorize("hasAuthority('PERM_AUDIT_MANAGEMENT')")
    public ResponseEntity<?> saveGraph(@RequestBody BranchingWorkflowSaveDto body) {
        try {
            return ResponseEntity.ok(demoBranchingWorkflowService.saveGraph(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
