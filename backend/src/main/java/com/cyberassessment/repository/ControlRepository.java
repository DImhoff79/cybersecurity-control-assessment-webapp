package com.cyberassessment.repository;

import com.cyberassessment.entity.Control;
import com.cyberassessment.entity.ControlFramework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlRepository extends JpaRepository<Control, Long> {

    List<Control> findByFramework(ControlFramework framework);

    List<Control> findByEnabled(Boolean enabled);

    List<Control> findByFrameworkAndEnabled(ControlFramework framework, Boolean enabled);

    boolean existsByControlIdAndFramework(String controlId, ControlFramework framework);
}
