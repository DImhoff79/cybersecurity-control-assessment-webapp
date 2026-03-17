package com.cyberassessment.repository;

import com.cyberassessment.entity.Regulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegulationRepository extends JpaRepository<Regulation, Long> {
    Optional<Regulation> findByCode(String code);
}
