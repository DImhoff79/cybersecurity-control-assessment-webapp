package com.cyberassessment.repository;

import com.cyberassessment.entity.ApplicationIntakeAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationIntakeAnswerRepository extends JpaRepository<ApplicationIntakeAnswer, Long> {

    List<ApplicationIntakeAnswer> findByApplication_IdOrderByQuestion_DisplayOrderAsc(Long applicationId);

    void deleteByApplication_Id(Long applicationId);
}
