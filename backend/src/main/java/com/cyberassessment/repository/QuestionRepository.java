package com.cyberassessment.repository;

import com.cyberassessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByControlIdOrderByDisplayOrderAsc(Long controlId);

    long countByControlId(Long controlId);

    Optional<Question> findFirstByQuestionTextIgnoreCase(String questionText);
}
