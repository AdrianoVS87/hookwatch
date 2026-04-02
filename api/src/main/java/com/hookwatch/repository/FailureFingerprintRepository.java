package com.hookwatch.repository;

import com.hookwatch.domain.FailureFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for failure fingerprint aggregation and trend queries.
 */
@Repository
public interface FailureFingerprintRepository extends JpaRepository<FailureFingerprint, UUID> {

    /**
     * Finds an existing fingerprint by its unique tenant+agent+hash composite.
     */
    Optional<FailureFingerprint> findByTenantIdAndAgentIdAndHash(UUID tenantId, UUID agentId, String hash);

    /**
     * Lists fingerprints for an agent, sorted by occurrence count descending.
     */
    List<FailureFingerprint> findByAgentIdOrderByOccurrenceCountDesc(UUID agentId);

    /**
     * Lists fingerprints for an agent within a tenant scope.
     */
    List<FailureFingerprint> findByTenantIdAndAgentIdOrderByOccurrenceCountDesc(UUID tenantId, UUID agentId);
}
