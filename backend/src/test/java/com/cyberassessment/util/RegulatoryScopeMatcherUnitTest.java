package com.cyberassessment.util;

import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.RegulatoryScopeTag;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RegulatoryScopeMatcherUnitTest {

    @Test
    void nullControlOrApp_applies() {
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(null, Application.builder().build())).isTrue();
        Control c = Control.builder().id(1L).build();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(c, null)).isTrue();
    }

    @Test
    void baselineControl_noTags_alwaysApplies() {
        Control c = Control.builder().id(1L).regulatoryScopes(new HashSet<>()).build();
        Application app = Application.builder()
                .dataScopePii(false)
                .dataScopePci(false)
                .build();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(c, app)).isTrue();
    }

    @Test
    void taggedControl_excludedWhenFlagExplicitlyFalse() {
        Set<RegulatoryScopeTag> pci = Set.of(RegulatoryScopeTag.PCI);
        Control c = Control.builder().id(1L).regulatoryScopes(pci).build();
        Application noPci = Application.builder().dataScopePci(false).build();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(c, noPci)).isFalse();
    }

    @Test
    void taggedControl_includedWhenFlagTrue() {
        Set<RegulatoryScopeTag> pci = Set.of(RegulatoryScopeTag.PCI);
        Control c = Control.builder().id(1L).regulatoryScopes(pci).build();
        Application yes = Application.builder().dataScopePci(true).build();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(c, yes)).isTrue();
    }

    @Test
    void taggedControl_includedWhenFlagNull_unknown() {
        Set<RegulatoryScopeTag> pii = Set.of(RegulatoryScopeTag.PII);
        Control c = Control.builder().id(1L).regulatoryScopes(pii).build();
        Application unknown = Application.builder().dataScopePii(null).build();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(c, unknown)).isTrue();
    }

    @Test
    void hipaaUsesHipaaThenPhiFallback() {
        Set<RegulatoryScopeTag> hipaa = Set.of(RegulatoryScopeTag.HIPAA);
        Control c = Control.builder().id(1L).regulatoryScopes(hipaa).build();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(
                c, Application.builder().dataScopeHipaa(false).dataScopePhi(null).build())).isFalse();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(
                c, Application.builder().dataScopeHipaa(null).dataScopePhi(false).build())).isFalse();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(
                c, Application.builder().dataScopeHipaa(true).build())).isTrue();
    }

    @Test
    void multipleTags_excludedIfAnyTagFalse() {
        Control c = Control.builder().id(1L)
                .regulatoryScopes(Set.of(RegulatoryScopeTag.PCI, RegulatoryScopeTag.PII))
                .build();
        Application app = Application.builder().dataScopePci(true).dataScopePii(false).build();
        assertThat(RegulatoryScopeMatcher.controlAppliesToApplication(c, app)).isFalse();
    }
}
