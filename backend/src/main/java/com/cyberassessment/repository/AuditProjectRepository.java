package com.cyberassessment.repository;

import com.cyberassessment.entity.AuditProject;
import com.cyberassessment.entity.AuditProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditProjectRepository extends JpaRepository<AuditProject, Long> {
    List<AuditProject> findAllByOrderByCreatedAtDesc();

    long countByStatus(AuditProjectStatus status);

    Optional<AuditProject> findFirstByName(String name);
}
