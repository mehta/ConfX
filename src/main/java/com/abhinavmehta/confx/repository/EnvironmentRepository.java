package com.abhinavmehta.confx.repository;

import com.abhinavmehta.confx.entity.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    Optional<Environment> findByProjectIdAndName(Long projectId, String name);
    List<Environment> findByProjectId(Long projectId);
    Optional<Environment> findByIdAndProjectId(Long environmentId, Long projectId);
} 