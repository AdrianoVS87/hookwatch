package com.hookwatch.repository;

import com.hookwatch.domain.Annotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, UUID> {
    List<Annotation> findByTrace_IdOrderByCreatedAtDesc(UUID traceId);
}
