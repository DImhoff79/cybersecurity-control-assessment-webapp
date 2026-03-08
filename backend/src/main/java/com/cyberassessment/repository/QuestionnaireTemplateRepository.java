package com.cyberassessment.repository;

import com.cyberassessment.entity.QuestionnaireTemplate;
import com.cyberassessment.entity.QuestionnaireTemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireTemplateRepository extends JpaRepository<QuestionnaireTemplate, Long> {
    Optional<QuestionnaireTemplate> findTopByStatusOrderByVersionNoDesc(QuestionnaireTemplateStatus status);
    Optional<QuestionnaireTemplate> findTopByOrderByVersionNoDesc();
    List<QuestionnaireTemplate> findAllByOrderByVersionNoDesc();
}
