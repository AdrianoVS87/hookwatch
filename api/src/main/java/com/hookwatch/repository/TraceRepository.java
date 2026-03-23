package com.hookwatch.repository;

import com.hookwatch.domain.Trace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TraceRepository extends JpaRepository<Trace, UUID> {

    Page<Trace> findByAgentId(UUID agentId, Pageable pageable);

    /**
     * Finds traces for an agent, verifying the agent belongs to the given tenant.
     * Prevents cross-tenant data access.
     */
    @Query("""
        SELECT t FROM Trace t
        JOIN Agent a ON a.id = t.agentId
        WHERE t.agentId = :agentId
          AND a.tenantId = :tenantId
    """)
    Page<Trace> findByAgentIdAndTenantId(@Param("agentId") UUID agentId,
                                          @Param("tenantId") UUID tenantId,
                                          Pageable pageable);

    /**
     * Finds a trace by ID, verifying the owning agent belongs to the given tenant.
     */
    @Query("""
        SELECT t FROM Trace t
        JOIN Agent a ON a.id = t.agentId
        WHERE t.id = :id
          AND a.tenantId = :tenantId
    """)
    Optional<Trace> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

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
