package com.cyberassessment.controller;

import com.cyberassessment.dto.ControlExceptionCreateRequest;
import com.cyberassessment.dto.ControlExceptionDecisionRequest;
import com.cyberassessment.dto.ControlExceptionDto;
import com.cyberassessment.dto.ControlExceptionUpdateRequest;
import com.cyberassessment.service.ControlExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Self-service and auditor-scoped control exceptions (participation-based access).
 */
@RestController
@RequestMapping("/api/workspace/control-exceptions")
@RequiredArgsConstructor
public class WorkspaceControlExceptionController {

    private final ControlExceptionService controlExceptionService;

    @GetMapping
    public List<ControlExceptionDto> list() {
        return controlExceptionService.listForWorkspace();
    }

    @PostMapping
    public ResponseEntity<ControlExceptionDto> request(@RequestBody ControlExceptionCreateRequest request) {
        return ResponseEntity.ok(controlExceptionService.requestForWorkspace(request));
    }

    @PutMapping("/{exceptionId}")
    public ResponseEntity<ControlExceptionDto> update(@PathVariable Long exceptionId,
                                                      @RequestBody ControlExceptionUpdateRequest request) {
        return ResponseEntity.ok(controlExceptionService.updateForWorkspace(exceptionId, request));
    }

    @PostMapping("/{exceptionId}/approve")
    public ResponseEntity<ControlExceptionDto> approve(@PathVariable Long exceptionId,
                                                       @RequestBody(required = false) ControlExceptionDecisionRequest request) {
        return ResponseEntity.ok(controlExceptionService.approve(exceptionId, request));
    }

    @PostMapping("/{exceptionId}/reject")
    public ResponseEntity<ControlExceptionDto> reject(@PathVariable Long exceptionId,
                                                      @RequestBody(required = false) ControlExceptionDecisionRequest request) {
        return ResponseEntity.ok(controlExceptionService.reject(exceptionId, request));
    }
}
