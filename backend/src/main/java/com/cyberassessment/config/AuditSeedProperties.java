package com.cyberassessment.config;

import com.cyberassessment.entity.ControlFramework;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Getter
public class AuditSeedProperties {

    /**
     * When true, enabled controls tagged with regulatory scopes are omitted from new audits if the application
     * explicitly marks that scope out-of-scope (false).
     */
    @Value("${app.audit.regulatory-scope-filter-enabled:false}")
    private boolean regulatoryScopeFilterEnabled;

    /**
     * When set (e.g. {@code KROGER_CCF}), new audits only include enabled controls from that framework so
     * assessment questions match the same catalog as the mapping studio. Empty = all enabled controls (legacy).
     */
    @Value("${app.audit.new-audit-framework:}")
    private String newAuditFramework;

    public Optional<ControlFramework> getNewAuditFrameworkFilter() {
        if (newAuditFramework == null || newAuditFramework.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ControlFramework.valueOf(newAuditFramework.trim()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
