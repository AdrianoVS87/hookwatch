package com.hookwatch.repository;

import com.hookwatch.domain.Trace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TraceRepository extends JpaRepository<Trace, UUID> {

    @EntityGraph(attributePaths = {"spans"})
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

    @Query(value = """
        SELECT t.*
        FROM traces t
        JOIN agents a ON a.id = t.agent_id
        WHERE t.agent_id = :agentId
          AND a.tenant_id = :tenantId
          AND :tag = ANY(t.tags)
        """, countQuery = """
        SELECT COUNT(*)
        FROM traces t
        JOIN agents a ON a.id = t.agent_id
        WHERE t.agent_id = :agentId
          AND a.tenant_id = :tenantId
          AND :tag = ANY(t.tags)
        """, nativeQuery = true)
    Page<Trace> findByAgentIdAndTenantIdAndTag(@Param("agentId") UUID agentId,
                                                @Param("tenantId") UUID tenantId,
                                                @Param("tag") String tag,
                                                Pageable pageable);

    @Query(value = """
        SELECT t.*
        FROM traces t
        WHERE t.agent_id = :agentId
          AND :tag = ANY(t.tags)
        """, countQuery = """
        SELECT COUNT(*)
        FROM traces t
        WHERE t.agent_id = :agentId
          AND :tag = ANY(t.tags)
        """, nativeQuery = true)
    Page<Trace> findByAgentIdAndTag(@Param("agentId") UUID agentId,
                                    @Param("tag") String tag,
                                    Pageable pageable);

    @Query(value = """
        SELECT DISTINCT tag
        FROM traces t
        JOIN agents a ON a.id = t.agent_id
        CROSS JOIN LATERAL unnest(t.tags) AS tag
        WHERE a.tenant_id = :tenantId
        ORDER BY tag
        """, nativeQuery = true)
    List<String> findUniqueTagsByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = """
        SELECT DISTINCT tag
        FROM traces t
        CROSS JOIN LATERAL unnest(t.tags) AS tag
        ORDER BY tag
        """, nativeQuery = true)
    List<String> findUniqueTags();

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
