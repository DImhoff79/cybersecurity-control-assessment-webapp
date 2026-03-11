package com.cyberassessment.service;

import com.cyberassessment.dto.AuditorSavedFilterDto;
import com.cyberassessment.entity.AuditorSavedFilter;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.repository.AuditorSavedFilterRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditorSavedFilterService {
    private final AuditorSavedFilterRepository auditorSavedFilterRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<AuditorSavedFilterDto> listForCurrentUser() {
        User current = currentUserService.getCurrentUserOrThrow();
        if (!currentUserService.hasPermission(UserPermission.REPORT_VIEW)) {
            throw new IllegalArgumentException("Missing permission: REPORT_VIEW");
        }
        return auditorSavedFilterRepository.findBySharedTrueOrCreatedByIdOrderByCreatedAtDesc(current.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AuditorSavedFilterDto create(String name, boolean shared, Map<String, Object> filterState) {
        User current = currentUserService.getCurrentUserOrThrow();
        if (!currentUserService.hasPermission(UserPermission.REPORT_VIEW)) {
            throw new IllegalArgumentException("Missing permission: REPORT_VIEW");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        try {
            String json = objectMapper.writeValueAsString(filterState != null ? filterState : Map.of());
            AuditorSavedFilter row = AuditorSavedFilter.builder()
                    .name(name.trim())
                    .shared(shared)
                    .filterJson(json)
                    .createdBy(current)
                    .build();
            row = auditorSavedFilterRepository.save(row);
            return toDto(row);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to save filter");
        }
    }

    @Transactional
    public void delete(Long id) {
        User current = currentUserService.getCurrentUserOrThrow();
        if (!currentUserService.hasPermission(UserPermission.REPORT_VIEW)) {
            throw new IllegalArgumentException("Missing permission: REPORT_VIEW");
        }
        AuditorSavedFilter row = auditorSavedFilterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Saved filter not found"));
        if (!row.getCreatedBy().getId().equals(current.getId()) && !Boolean.TRUE.equals(row.getShared())) {
            throw new IllegalArgumentException("You can only delete your own private filters");
        }
        auditorSavedFilterRepository.delete(row);
    }

    private AuditorSavedFilterDto toDto(AuditorSavedFilter row) {
        try {
            Map<String, Object> state = objectMapper.readValue(row.getFilterJson(), new TypeReference<>() {});
            return AuditorSavedFilterDto.builder()
                    .id(row.getId())
                    .name(row.getName())
                    .shared(row.getShared())
                    .createdByUserId(row.getCreatedBy().getId())
                    .createdByEmail(row.getCreatedBy().getEmail())
                    .createdAt(row.getCreatedAt())
                    .filterState(state)
                    .build();
        } catch (Exception e) {
            return AuditorSavedFilterDto.builder()
                    .id(row.getId())
                    .name(row.getName())
                    .shared(row.getShared())
                    .createdByUserId(row.getCreatedBy().getId())
                    .createdByEmail(row.getCreatedBy().getEmail())
                    .createdAt(row.getCreatedAt())
                    .filterState(Map.of())
                    .build();
        }
    }
}
