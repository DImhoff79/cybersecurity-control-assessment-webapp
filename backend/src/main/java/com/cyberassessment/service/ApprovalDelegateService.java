package com.cyberassessment.service;

import com.cyberassessment.dto.ApprovalDelegateDto;
import com.cyberassessment.entity.ApprovalDelegate;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.ApprovalDelegateRepository;
import com.cyberassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalDelegateService {

    private final ApprovalDelegateRepository approvalDelegateRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ApprovalDelegateDto> list() {
        return approvalDelegateRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(ApprovalDelegateService::toDto)
                .toList();
    }

    @Transactional
    public ApprovalDelegateDto add(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (approvalDelegateRepository.existsByUser_Id(userId)) {
            throw new IllegalArgumentException("User is already an approval delegate");
        }
        ApprovalDelegate row = ApprovalDelegate.builder()
                .user(user)
                .createdAt(Instant.now())
                .build();
        row = approvalDelegateRepository.save(row);
        return toDto(row);
    }

    @Transactional
    public void remove(Long id) {
        ApprovalDelegate row = approvalDelegateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Delegate not found"));
        approvalDelegateRepository.delete(row);
    }

    private static ApprovalDelegateDto toDto(ApprovalDelegate row) {
        User u = row.getUser();
        return ApprovalDelegateDto.builder()
                .id(row.getId())
                .userId(u != null ? u.getId() : null)
                .email(u != null ? u.getEmail() : null)
                .displayName(u != null ? u.getDisplayName() : null)
                .build();
    }
}
