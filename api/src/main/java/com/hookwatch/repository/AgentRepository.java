package com.hookwatch.repository;

import com.hookwatch.domain.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {

    /** Returns only agents belonging to the specified tenant. */
    List<Agent> findByTenantId(UUID tenantId);

    /** Finds an agent by ID, enforcing that it belongs to the specified tenant. */
    Optional<Agent> findByIdAndTenantId(UUID id, UUID tenantId);
}
