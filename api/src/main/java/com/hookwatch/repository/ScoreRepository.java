package com.hookwatch.repository;

import com.hookwatch.domain.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScoreRepository extends JpaRepository<Score, UUID> {

    List<Score> findByTraceId(UUID traceId);

    boolean existsByTraceIdAndName(UUID traceId, String name);

    @Query("""
        SELECT s FROM Score s
        JOIN s.trace t
        WHERE t.agentId = :agentId
    """)
    List<Score> findByAgentId(@Param("agentId") UUID agentId);

    @Query("""
        SELECT s.name, AVG(s.numericValue)
        FROM Score s
        JOIN s.trace t
        WHERE t.agentId = :agentId AND s.dataType = 'NUMERIC'
        GROUP BY s.name
    """)
    List<Object[]> avgNumericScoresByAgentId(@Param("agentId") UUID agentId);

    @Query("""
        SELECT s.name, s.stringValue, COUNT(s)
        FROM Score s
        JOIN s.trace t
        WHERE t.agentId = :agentId AND s.dataType = 'CATEGORICAL'
        GROUP BY s.name, s.stringValue
    """)
    List<Object[]> categoricalDistributionByAgentId(@Param("agentId") UUID agentId);
}
