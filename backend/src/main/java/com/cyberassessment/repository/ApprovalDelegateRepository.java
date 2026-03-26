package com.cyberassessment.repository;

import com.cyberassessment.entity.ApprovalDelegate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalDelegateRepository extends JpaRepository<ApprovalDelegate, Long> {
    boolean existsByUser_Id(Long userId);

    Optional<ApprovalDelegate> findByUser_Id(Long userId);

    List<ApprovalDelegate> findAllByOrderByCreatedAtAsc();
}
