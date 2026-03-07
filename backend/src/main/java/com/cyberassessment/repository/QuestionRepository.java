package com.cyberassessment.repository;

import com.cyberassessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByControlIdOrderByDisplayOrderAsc(Long controlId);
}
