package com.cyberassessment.repository;

import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByOwner(User owner);

    Optional<Application> findFirstByName(String name);

    long countByNameStartingWith(String prefix);
}
