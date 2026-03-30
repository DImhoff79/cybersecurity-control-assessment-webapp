package com.cyberassessment.entity;

/**
 * When assigned to a {@link Control}, the control is included in owner assessments only if
 * the application's triage flags do not explicitly exclude every tagged scope (see AuditService).
 * Controls with no tags remain baseline (always included when enabled).
 */
public enum RegulatoryScopeTag {
    PII,
    PCI,
    SOX,
    HIPAA
}
