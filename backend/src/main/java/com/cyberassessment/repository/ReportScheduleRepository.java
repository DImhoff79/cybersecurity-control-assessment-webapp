package com.cyberassessment.repository;

import com.cyberassessment.entity.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {
    List<ReportSchedule> findByActiveTrueAndNextRunAtBefore(Instant nextRunAt);
}
