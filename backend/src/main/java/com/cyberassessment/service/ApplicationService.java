package com.cyberassessment.service;

import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.entity.ApplicationCriticality;
import com.cyberassessment.entity.ApplicationLifecycleStatus;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.ApplicationPurpose;
import com.cyberassessment.entity.DataClassification;
import com.cyberassessment.entity.HostingModel;
import com.cyberassessment.entity.IntegrationScope;
import com.cyberassessment.entity.UserAudienceScope;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public static ApplicationDto toDto(Application a) {
        if (a == null) return null;
        User owner = a.getOwner();
        Boolean hipaa = a.getDataScopeHipaa() != null ? a.getDataScopeHipaa() : a.getDataScopePhi();
        return ApplicationDto.builder()
                .id(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .ownerId(owner != null ? owner.getId() : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerDisplayName(owner != null ? owner.getDisplayName() : null)
                .criticality(a.getCriticality())
                .dataClassification(a.getDataClassification())
                .regulatoryScope(a.getRegulatoryScope())
                .businessOwnerName(a.getBusinessOwnerName())
                .technicalOwnerName(a.getTechnicalOwnerName())
                .lifecycleStatus(a.getLifecycleStatus())
                .applicationPurpose(a.getApplicationPurpose())
                .hostingModel(a.getHostingModel())
                .userAudienceScope(a.getUserAudienceScope())
                .integrationScope(a.getIntegrationScope())
                .dataScopePii(a.getDataScopePii())
                .dataScopePhi(hipaa)
                .dataScopePci(a.getDataScopePci())
                .dataScopeSox(a.getDataScopeSox())
                .dataScopeHipaa(hipaa)
                .createdAt(a.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> findAll() {
        return applicationRepository.findAll().stream().map(ApplicationService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationDto findById(Long id) {
        return applicationRepository.findById(id).map(ApplicationService::toDto).orElse(null);
    }

    @Transactional
    public ApplicationDto create(String name, String description, Long ownerId) {
        return create(toCreateMap(name, description, ownerId, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null));
    }

    @Transactional
    public ApplicationDto create(String name, String description, Long ownerId,
                                 ApplicationCriticality criticality, DataClassification dataClassification,
                                 String regulatoryScope, String businessOwnerName, String technicalOwnerName,
                                 ApplicationLifecycleStatus lifecycleStatus) {
        return create(toCreateMap(name, description, ownerId, criticality, dataClassification, regulatoryScope,
                businessOwnerName, technicalOwnerName, lifecycleStatus,
                null, null, null, null, null, null, null, null));
    }

    @Transactional
    public ApplicationDto create(Map<String, Object> body) {
        Object nameObj = body.get("name");
        if (nameObj == null || String.valueOf(nameObj).isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        Application app = Application.builder()
                .name(String.valueOf(nameObj).trim())
                .build();
        mergeFromBody(app, body, false);
        app = applicationRepository.save(app);
        return toDto(app);
    }

    @Transactional
    public ApplicationDto update(Long id, String name, String description, Long ownerId) {
        return update(id, name, description, ownerId, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    @Transactional
    public ApplicationDto update(Long id, String name, String description, Long ownerId,
                                 ApplicationCriticality criticality, DataClassification dataClassification,
                                 String regulatoryScope, String businessOwnerName, String technicalOwnerName,
                                 ApplicationLifecycleStatus lifecycleStatus) {
        return update(id, toUpdateMap(name, description, ownerId, criticality, dataClassification, regulatoryScope,
                businessOwnerName, technicalOwnerName, lifecycleStatus,
                null, null, null, null, null, null, null, null));
    }

    @Transactional
    public ApplicationDto update(Long id, Map<String, Object> body) {
        Application app = applicationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
        mergeFromBody(app, body, true);
        app = applicationRepository.save(app);
        return toDto(app);
    }

    /**
     * @param partial true = PUT semantics (only keys present in body are applied)
     */
    private void mergeFromBody(Application app, Map<String, Object> body, boolean partial) {
        if (!partial || body.containsKey("name")) {
            Object n = body.get("name");
            if (n != null) {
                app.setName(String.valueOf(n).trim());
            }
        }
        if (!partial || body.containsKey("description")) {
            app.setDescription((String) body.get("description"));
        }
        if (!partial || body.containsKey("ownerId")) {
            Object oid = body.get("ownerId");
            if (oid == null) {
                app.setOwner(null);
            } else {
                Long ownerId = ((Number) oid).longValue();
                app.setOwner(userRepository.findById(ownerId).orElse(null));
            }
        }
        if (!partial || body.containsKey("criticality")) {
            app.setCriticality(parseEnum(body.get("criticality"), ApplicationCriticality.class));
        }
        if (!partial || body.containsKey("dataClassification")) {
            app.setDataClassification(parseEnum(body.get("dataClassification"), DataClassification.class));
        }
        if (!partial || body.containsKey("regulatoryScope")) {
            app.setRegulatoryScope((String) body.get("regulatoryScope"));
        }
        if (!partial || body.containsKey("businessOwnerName")) {
            app.setBusinessOwnerName((String) body.get("businessOwnerName"));
        }
        if (!partial || body.containsKey("technicalOwnerName")) {
            app.setTechnicalOwnerName((String) body.get("technicalOwnerName"));
        }
        if (!partial || body.containsKey("lifecycleStatus")) {
            app.setLifecycleStatus(parseEnum(body.get("lifecycleStatus"), ApplicationLifecycleStatus.class));
        }
        if (!partial || body.containsKey("applicationPurpose")) {
            app.setApplicationPurpose(parseEnum(body.get("applicationPurpose"), ApplicationPurpose.class));
        }
        if (!partial || body.containsKey("hostingModel")) {
            app.setHostingModel(parseEnum(body.get("hostingModel"), HostingModel.class));
        }
        if (!partial || body.containsKey("userAudienceScope")) {
            app.setUserAudienceScope(parseEnum(body.get("userAudienceScope"), UserAudienceScope.class));
        }
        if (!partial || body.containsKey("integrationScope")) {
            app.setIntegrationScope(parseEnum(body.get("integrationScope"), IntegrationScope.class));
        }
        if (!partial || body.containsKey("dataScopePii")) {
            app.setDataScopePii(parseBooleanObject(body.get("dataScopePii")));
        }
        if (!partial || body.containsKey("dataScopePci")) {
            app.setDataScopePci(parseBooleanObject(body.get("dataScopePci")));
        }
        if (!partial || body.containsKey("dataScopeSox")) {
            app.setDataScopeSox(parseBooleanObject(body.get("dataScopeSox")));
        }
        if (!partial || body.containsKey("dataScopeHipaa")) {
            Boolean h = parseBooleanObject(body.get("dataScopeHipaa"));
            app.setDataScopeHipaa(h);
            app.setDataScopePhi(h);
        } else if (!partial || body.containsKey("dataScopePhi")) {
            Boolean p = parseBooleanObject(body.get("dataScopePhi"));
            app.setDataScopePhi(p);
            app.setDataScopeHipaa(p);
        }
    }

    private static Boolean parseBooleanObject(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        return null;
    }

    private static <E extends Enum<E>> E parseEnum(Object raw, Class<E> type) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            if (s.isBlank()) {
                return null;
            }
            return Enum.valueOf(type, s);
        }
        return null;
    }

    private static Map<String, Object> toCreateMap(String name, String description, Long ownerId,
                                                   ApplicationCriticality criticality, DataClassification dataClassification,
                                                   String regulatoryScope, String businessOwnerName, String technicalOwnerName,
                                                   ApplicationLifecycleStatus lifecycleStatus,
                                                   ApplicationPurpose applicationPurpose, HostingModel hostingModel,
                                                   UserAudienceScope userAudienceScope, IntegrationScope integrationScope,
                                                   Boolean dataScopePii, Boolean dataScopePci, Boolean dataScopeSox,
                                                   Boolean dataScopeHipaa) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("ownerId", ownerId);
        m.put("criticality", criticality != null ? criticality.name() : null);
        m.put("dataClassification", dataClassification != null ? dataClassification.name() : null);
        m.put("regulatoryScope", regulatoryScope);
        m.put("businessOwnerName", businessOwnerName);
        m.put("technicalOwnerName", technicalOwnerName);
        m.put("lifecycleStatus", lifecycleStatus != null ? lifecycleStatus.name() : null);
        m.put("applicationPurpose", applicationPurpose != null ? applicationPurpose.name() : null);
        m.put("hostingModel", hostingModel != null ? hostingModel.name() : null);
        m.put("userAudienceScope", userAudienceScope != null ? userAudienceScope.name() : null);
        m.put("integrationScope", integrationScope != null ? integrationScope.name() : null);
        m.put("dataScopePii", dataScopePii);
        m.put("dataScopePci", dataScopePci);
        m.put("dataScopeSox", dataScopeSox);
        m.put("dataScopeHipaa", dataScopeHipaa);
        return m;
    }

    private static Map<String, Object> toUpdateMap(String name, String description, Long ownerId,
                                                   ApplicationCriticality criticality, DataClassification dataClassification,
                                                   String regulatoryScope, String businessOwnerName, String technicalOwnerName,
                                                   ApplicationLifecycleStatus lifecycleStatus,
                                                   ApplicationPurpose applicationPurpose, HostingModel hostingModel,
                                                   UserAudienceScope userAudienceScope, IntegrationScope integrationScope,
                                                   Boolean dataScopePii, Boolean dataScopePci, Boolean dataScopeSox,
                                                   Boolean dataScopeHipaa) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (name != null) {
            m.put("name", name);
        }
        if (description != null) {
            m.put("description", description);
        }
        if (ownerId != null) {
            m.put("ownerId", ownerId);
        }
        if (criticality != null) {
            m.put("criticality", criticality.name());
        }
        if (dataClassification != null) {
            m.put("dataClassification", dataClassification.name());
        }
        if (regulatoryScope != null) {
            m.put("regulatoryScope", regulatoryScope);
        }
        if (businessOwnerName != null) {
            m.put("businessOwnerName", businessOwnerName);
        }
        if (technicalOwnerName != null) {
            m.put("technicalOwnerName", technicalOwnerName);
        }
        if (lifecycleStatus != null) {
            m.put("lifecycleStatus", lifecycleStatus.name());
        }
        if (applicationPurpose != null) {
            m.put("applicationPurpose", applicationPurpose.name());
        }
        if (hostingModel != null) {
            m.put("hostingModel", hostingModel.name());
        }
        if (userAudienceScope != null) {
            m.put("userAudienceScope", userAudienceScope.name());
        }
        if (integrationScope != null) {
            m.put("integrationScope", integrationScope.name());
        }
        if (dataScopePii != null) {
            m.put("dataScopePii", dataScopePii);
        }
        if (dataScopePci != null) {
            m.put("dataScopePci", dataScopePci);
        }
        if (dataScopeSox != null) {
            m.put("dataScopeSox", dataScopeSox);
        }
        if (dataScopeHipaa != null) {
            m.put("dataScopeHipaa", dataScopeHipaa);
            m.put("dataScopePhi", dataScopeHipaa);
        }
        return m;
    }

    @Transactional
    public ApplicationDto update(Long id, String name, String description, Long ownerId,
                                 ApplicationCriticality criticality, DataClassification dataClassification,
                                 String regulatoryScope, String businessOwnerName, String technicalOwnerName,
                                 ApplicationLifecycleStatus lifecycleStatus,
                                 ApplicationPurpose applicationPurpose, HostingModel hostingModel,
                                 UserAudienceScope userAudienceScope, IntegrationScope integrationScope,
                                 Boolean dataScopePii, Boolean dataScopePci, Boolean dataScopeSox, Boolean dataScopeHipaa) {
        return update(id, toUpdateMap(name, description, ownerId, criticality, dataClassification, regulatoryScope,
                businessOwnerName, technicalOwnerName, lifecycleStatus,
                applicationPurpose, hostingModel, userAudienceScope, integrationScope,
                dataScopePii, dataScopePci, dataScopeSox, dataScopeHipaa));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!applicationRepository.existsById(id)) {
            throw new IllegalArgumentException("Application not found: " + id);
        }
        applicationRepository.deleteById(id);
    }
}
