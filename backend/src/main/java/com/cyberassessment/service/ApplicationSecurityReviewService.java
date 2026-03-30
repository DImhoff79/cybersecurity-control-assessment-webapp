package com.cyberassessment.service;

import com.cyberassessment.dto.ApplicationSecurityReviewSummaryDto;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.ApplicationSecurityReview;
import com.cyberassessment.entity.ApplicationSecurityReviewStatus;
import com.cyberassessment.entity.User;
import com.cyberassessment.entity.UserPermission;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.ApplicationSecurityReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationSecurityReviewService {

    private final ApplicationSecurityReviewRepository reviewRepository;
    private final ApplicationRepository applicationRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void ensureNotStartedRow(Application application) {
        if (application == null || application.getId() == null) {
            return;
        }
        if (reviewRepository.findByApplication_Id(application.getId()).isPresent()) {
            return;
        }
        ApplicationSecurityReview row = ApplicationSecurityReview.builder()
                .application(application)
                .status(ApplicationSecurityReviewStatus.NOT_STARTED)
                .build();
        reviewRepository.save(row);
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationSecurityReviewSummaryDto> findSummary(Long applicationId) {
        return reviewRepository.findByApplication_Id(applicationId).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Map<Long, ApplicationSecurityReviewSummaryDto> findSummariesByApplicationIds(Collection<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ApplicationSecurityReviewSummaryDto> out = new HashMap<>();
        for (ApplicationSecurityReview r : reviewRepository.findByApplication_IdIn(applicationIds)) {
            out.put(r.getApplication().getId(), toSummary(r));
        }
        return out;
    }

    private ApplicationSecurityReviewSummaryDto toSummary(ApplicationSecurityReview r) {
        User by = r.getReviewedBy();
        return ApplicationSecurityReviewSummaryDto.builder()
                .status(r.getStatus().name())
                .notes(r.getNotes())
                .reviewedAt(r.getReviewedAt())
                .reviewedByDisplayName(by != null ? by.getDisplayName() : null)
                .build();
    }

    @Transactional
    public ApplicationSecurityReviewSummaryDto updateStatus(Long applicationId, ApplicationSecurityReviewStatus status, String notes) {
        if (!currentUserService.hasPermission(UserPermission.AUDIT_MANAGEMENT) && !currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Missing permission to update security architecture review");
        }
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        ApplicationSecurityReview row = reviewRepository.findByApplication_Id(applicationId)
                .orElseGet(() -> ApplicationSecurityReview.builder()
                        .application(app)
                        .status(ApplicationSecurityReviewStatus.NOT_STARTED)
                        .build());
        User me = currentUserService.getCurrentUserOrThrow();
        if (status != null) {
            row.setStatus(status);
            if (status == ApplicationSecurityReviewStatus.APPROVED || status == ApplicationSecurityReviewStatus.CHANGES_REQUESTED) {
                row.setReviewedBy(me);
                row.setReviewedAt(Instant.now());
            }
        }
        if (notes != null) {
            row.setNotes(notes);
        }
        reviewRepository.save(row);
        return toSummary(row);
    }
}
