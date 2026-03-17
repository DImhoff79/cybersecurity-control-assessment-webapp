package com.cyberassessment.entity;

import java.util.EnumSet;
import java.util.Set;

public enum UserRole {
    ADMIN,
    AUDIT_MANAGER,
    AUDITOR,
    APPLICATION_OWNER;

    public Set<UserPermission> defaultPermissions() {
        return switch (this) {
            case ADMIN -> EnumSet.allOf(UserPermission.class);
            case AUDIT_MANAGER -> EnumSet.of(
                    UserPermission.USER_MANAGEMENT,
                    UserPermission.AUDIT_MANAGEMENT,
                    UserPermission.REPORT_VIEW,
                    UserPermission.POLICY_MANAGEMENT,
                    UserPermission.COMPLIANCE_MANAGEMENT,
                    UserPermission.RISK_MANAGEMENT,
                    UserPermission.REMEDIATION_MANAGEMENT
            );
            case AUDITOR -> EnumSet.of(
                    UserPermission.AUDIT_EXECUTION,
                    UserPermission.REPORT_VIEW,
                    UserPermission.COMPLIANCE_MANAGEMENT,
                    UserPermission.REMEDIATION_MANAGEMENT
            );
            case APPLICATION_OWNER -> EnumSet.of(
                    UserPermission.APPLICATION_MANAGEMENT,
                    UserPermission.AUDIT_EXECUTION,
                    UserPermission.REPORT_VIEW
            );
        };
    }
}
