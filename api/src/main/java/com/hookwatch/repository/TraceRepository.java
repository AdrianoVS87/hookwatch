package com.hookwatch.repository;

import com.hookwatch.domain.Trace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TraceRepository extends JpaRepository<Trace, UUID> {

    Page<Trace> findByAgentId(UUID agentId, Pageable pageable);

    @Query("""
        SELECT COUNT(t) FROM Trace t WHERE t.agentId = :agentId
    """)
    long countByAgentId(@Param("agentId") UUID agentId);

    @Query("""
        SELECT AVG(t.totalTokens) FROM Trace t WHERE t.agentId = :agentId AND t.totalTokens IS NOT NULL
    """)
    Double avgTokensByAgentId(@Param("agentId") UUID agentId);

    @Query("""
        SELECT AVG(t.totalCost) FROM Trace t WHERE t.agentId = :agentId AND t.totalCost IS NOT NULL
    """)
    Double avgCostByAgentId(@Param("agentId") UUID agentId);

    @Query("""
        SELECT COUNT(t) FROM Trace t WHERE t.agentId = :agentId AND t.status = 'COMPLETED'
    """)
    long countCompletedByAgentId(@Param("agentId") UUID agentId);
}
