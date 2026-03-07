package com.cyberassessment.repository;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<Audit, Long> {

    List<Audit> findByApplicationOrderByYearDesc(Application application);

    Optional<Audit> findByApplicationIdAndYear(Long applicationId, Integer year);

    List<Audit> findByAssignedTo(User user);

    List<Audit> findByAssignedToId(Long userId);
}
