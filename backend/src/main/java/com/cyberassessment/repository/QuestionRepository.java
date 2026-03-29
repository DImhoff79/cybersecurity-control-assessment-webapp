package com.cyberassessment.repository;

import com.cyberassessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByControlIdOrderByDisplayOrderAsc(Long controlId);

    /** Per control, from linked questions (via mappings; library questions may omit Question.control FK). */
    @Query("""
            SELECT m.control.id,
                   SUM(CASE WHEN m.question.askOwner = true THEN 1 ELSE 0 END),
                   COUNT(m)
            FROM QuestionControlMapping m
            WHERE m.control.id IN :ids
            GROUP BY m.control.id
            """)
    List<Object[]> summarizeAskOwnerByControlIds(@Param("ids") Collection<Long> ids);

    long countByControlId(Long controlId);

    Optional<Question> findFirstByQuestionTextIgnoreCase(String questionText);
}
