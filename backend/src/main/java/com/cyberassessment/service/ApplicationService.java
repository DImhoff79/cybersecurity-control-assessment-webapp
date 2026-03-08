package com.cyberassessment.service;

import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.entity.ApplicationCriticality;
import com.cyberassessment.entity.ApplicationLifecycleStatus;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.DataClassification;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public static ApplicationDto toDto(Application a) {
        if (a == null) return null;
        User owner = a.getOwner();
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
        return create(name, description, ownerId, null, null, null, null, null, null);
    }

    @Transactional
    public ApplicationDto create(String name, String description, Long ownerId,
                                 ApplicationCriticality criticality, DataClassification dataClassification,
                                 String regulatoryScope, String businessOwnerName, String technicalOwnerName,
                                 ApplicationLifecycleStatus lifecycleStatus) {
        User owner = ownerId != null ? userRepository.findById(ownerId).orElse(null) : null;
        Application app = Application.builder()
                .name(name)
                .description(description)
                .owner(owner)
                .criticality(criticality)
                .dataClassification(dataClassification)
                .regulatoryScope(regulatoryScope)
                .businessOwnerName(businessOwnerName)
                .technicalOwnerName(technicalOwnerName)
                .lifecycleStatus(lifecycleStatus)
                .build();
        app = applicationRepository.save(app);
        return toDto(app);
    }

    @Transactional
    public ApplicationDto update(Long id, String name, String description, Long ownerId) {
        return update(id, name, description, ownerId, null, null, null, null, null, null);
    }

    @Transactional
    public ApplicationDto update(Long id, String name, String description, Long ownerId,
                                 ApplicationCriticality criticality, DataClassification dataClassification,
                                 String regulatoryScope, String businessOwnerName, String technicalOwnerName,
                                 ApplicationLifecycleStatus lifecycleStatus) {
        Application app = applicationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
        if (name != null) app.setName(name);
        if (description != null) app.setDescription(description);
        if (ownerId != null) {
            app.setOwner(userRepository.findById(ownerId).orElse(null));
        }
        if (criticality != null) app.setCriticality(criticality);
        if (dataClassification != null) app.setDataClassification(dataClassification);
        if (regulatoryScope != null) app.setRegulatoryScope(regulatoryScope);
        if (businessOwnerName != null) app.setBusinessOwnerName(businessOwnerName);
        if (technicalOwnerName != null) app.setTechnicalOwnerName(technicalOwnerName);
        if (lifecycleStatus != null) app.setLifecycleStatus(lifecycleStatus);
        app = applicationRepository.save(app);
        return toDto(app);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!applicationRepository.existsById(id)) {
            throw new IllegalArgumentException("Application not found: " + id);
        }
        applicationRepository.deleteById(id);
    }
}
