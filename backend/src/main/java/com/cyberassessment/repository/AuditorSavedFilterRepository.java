package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditorSavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditorSavedFilterRepository extends JpaRepository<AuditorSavedFilter, Long> {
    List<AuditorSavedFilter> findBySharedTrueOrCreatedByIdOrderByCreatedAtDesc(Long createdById);
}
