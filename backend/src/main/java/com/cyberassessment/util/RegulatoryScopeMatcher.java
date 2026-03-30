package com.cyberassessment.util;

import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.RegulatoryScopeTag;

import java.util.Set;

public final class RegulatoryScopeMatcher {

    private RegulatoryScopeMatcher() {}

    /**
     * Baseline controls (no tags) always apply. Tagged controls apply unless any tag is explicitly ruled out
     * by a {@code false} flag on the application (null flags are treated as unknown / still in scope).
     */
    public static boolean controlAppliesToApplication(Control control, Application app) {
        if (control == null || app == null) {
            return true;
        }
        Set<RegulatoryScopeTag> tags = control.getRegulatoryScopes();
        if (tags == null || tags.isEmpty()) {
            return true;
        }
        for (RegulatoryScopeTag tag : tags) {
            Boolean flag = flagForTag(app, tag);
            if (Boolean.FALSE.equals(flag)) {
                return false;
            }
        }
        return true;
    }

    private static Boolean flagForTag(Application app, RegulatoryScopeTag tag) {
        return switch (tag) {
            case PII -> app.getDataScopePii();
            case PCI -> app.getDataScopePci();
            case SOX -> app.getDataScopeSox();
            case HIPAA -> app.getDataScopeHipaa() != null ? app.getDataScopeHipaa() : app.getDataScopePhi();
        };
    }
}
