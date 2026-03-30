package com.cyberassessment.repository;

import com.cyberassessment.entity.ApplicationSecurityReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationSecurityReviewRepository extends JpaRepository<ApplicationSecurityReview, Long> {

    Optional<ApplicationSecurityReview> findByApplication_Id(Long applicationId);

    List<ApplicationSecurityReview> findByApplication_IdIn(Collection<Long> applicationIds);
}
