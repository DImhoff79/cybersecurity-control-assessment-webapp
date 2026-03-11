package com.cyberassessment.repository;

import com.cyberassessment.entity.Audit;
import com.cyberassessment.entity.AuditStatus;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<Audit, Long> {

    List<Audit> findByApplicationOrderByYearDesc(Application application);

    Optional<Audit> findByApplicationIdAndYear(Long applicationId, Integer year);

    List<Audit> findByAssignedTo(User user);

    List<Audit> findByAssignedToId(Long userId);

    List<Audit> findByStatusIn(List<AuditStatus> statuses);

    long countByStatus(AuditStatus status);

    long countByStatusIn(List<AuditStatus> statuses);

    long countByDueAtBeforeAndStatusIn(java.time.Instant dueAt, List<AuditStatus> statuses);

    long countByDueAtBeforeAndStatusNot(java.time.Instant dueAt, AuditStatus status);

    List<Audit> findTop12ByDueAtBeforeAndStatusNotOrderByDueAtAsc(java.time.Instant dueAt, AuditStatus status);

    Page<Audit> findByStatusInOrderByDueAtAsc(List<AuditStatus> statuses, Pageable pageable);

    @Query("select a from Audit a order by a.id desc")
    Page<Audit> findAllPaged(Pageable pageable);
}
