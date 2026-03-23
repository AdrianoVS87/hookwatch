package com.hookwatch.repository;

import com.hookwatch.domain.Span;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SpanRepository extends JpaRepository<Span, UUID> {
}
