package com.cyberassessment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AuditSeedProperties {

    /**
     * When true, enabled controls tagged with regulatory scopes are omitted from new audits if the application
     * explicitly marks that scope out-of-scope (false).
     */
    @Value("${app.audit.regulatory-scope-filter-enabled:false}")
    private boolean regulatoryScopeFilterEnabled;
}
