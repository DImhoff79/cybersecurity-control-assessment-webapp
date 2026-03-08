package com.cyberassessment.repository;

import com.cyberassessment.entity.QuestionnaireTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionnaireTemplateItemRepository extends JpaRepository<QuestionnaireTemplateItem, Long> {
    List<QuestionnaireTemplateItem> findByTemplateIdOrderByDisplayOrderAscControlControlIdAsc(Long templateId);
}
