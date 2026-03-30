package com.cyberassessment.repository;

import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.ControlFramework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ControlRepository extends JpaRepository<Control, Long> {

    List<Control> findByFramework(ControlFramework framework);

    List<Control> findByEnabled(Boolean enabled);

    List<Control> findByFrameworkAndEnabled(ControlFramework framework, Boolean enabled);

    boolean existsByControlIdAndFramework(String controlId, ControlFramework framework);

    @Query("SELECT DISTINCT c FROM Control c LEFT JOIN FETCH c.regulatoryScopes WHERE c.enabled = true")
    List<Control> findAllEnabledWithRegulatoryScopes();
}
