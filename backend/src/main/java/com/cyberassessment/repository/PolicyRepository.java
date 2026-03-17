package com.cyberassessment.repository;

import com.cyberassessment.entity.Policy;
import com.cyberassessment.entity.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByCode(String code);
    List<Policy> findByStatusInOrderByUpdatedAtDesc(List<PolicyStatus> statuses);
}
