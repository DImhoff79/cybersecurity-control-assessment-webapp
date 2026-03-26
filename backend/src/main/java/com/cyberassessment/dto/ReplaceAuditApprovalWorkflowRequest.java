package com.cyberassessment.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReplaceAuditApprovalWorkflowRequest {
    /** Ordered list of auditor user IDs (must have role AUDITOR). */
    private List<Long> assigneeUserIds;
}
