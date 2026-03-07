package com.cyberassessment.repository;

import com.cyberassessment.entity.QuestionControlId;
import com.cyberassessment.entity.QuestionControlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionControlMappingRepository extends JpaRepository<QuestionControlMapping, QuestionControlId> {

    List<QuestionControlMapping> findByControl_IdOrderByQuestionDisplayOrderAsc(Long controlId);

    List<QuestionControlMapping> findByQuestion_Id(Long questionId);

    long countByControl_Id(Long controlId);

    long countByQuestion_Id(Long questionId);

    boolean existsByControl_IdAndQuestion_Id(Long controlId, Long questionId);
}
