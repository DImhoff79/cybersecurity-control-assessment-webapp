package com.cyberassessment.entity;

public enum AuditStatus {
    DRAFT,
    IN_PROGRESS,
    /** Owner submitted; awaiting auditor approval workflow */
    PENDING_APPROVAL,
    /** Auditor requested more information; owner may edit and resubmit */
    REVISIONS_REQUESTED,
    /** All approval steps completed; ready for attestation */
    AUDITOR_APPROVED,
    ATTESTED,
    COMPLETE
}
