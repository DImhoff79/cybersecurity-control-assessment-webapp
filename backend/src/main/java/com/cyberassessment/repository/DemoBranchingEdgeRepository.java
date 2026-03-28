package com.cyberassessment.repository;

import com.cyberassessment.entity.DemoBranchingEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DemoBranchingEdgeRepository extends JpaRepository<DemoBranchingEdge, Long> {

    List<DemoBranchingEdge> findByFromNodeIdOrderBySortOrderAsc(Long fromNodeId);

    List<DemoBranchingEdge> findByVersion_IdOrderByIdAsc(Long versionId);

    @Query(
            "SELECT e FROM DemoBranchingEdge e JOIN FETCH e.fromNode JOIN FETCH e.toNode "
                    + "WHERE e.version.id = :vid ORDER BY e.sortOrder ASC, e.id ASC")
    List<DemoBranchingEdge> findByVersion_IdWithEndpoints(@Param("vid") Long versionId);
}
