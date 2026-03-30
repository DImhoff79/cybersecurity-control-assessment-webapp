package com.cyberassessment.repository;

import com.cyberassessment.entity.OwnerAnswerOptionProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnerAnswerOptionProfileRepository extends JpaRepository<OwnerAnswerOptionProfile, Long> {

    Optional<OwnerAnswerOptionProfile> findByCode(String code);

    List<OwnerAnswerOptionProfile> findAllByOrderByDisplayNameAsc();
}
