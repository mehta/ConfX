package com.abhinavmehta.confx.repository;

import com.abhinavmehta.confx.entity.ConfigDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigDependencyRepository extends JpaRepository<ConfigDependency, Long> {
    List<ConfigDependency> findByDependentConfigItemId(Long dependentConfigItemId);
    List<ConfigDependency> findByPrerequisiteConfigItemId(Long prerequisiteConfigItemId);
    Optional<ConfigDependency> findByDependentConfigItemIdAndPrerequisiteConfigItemId(Long dependentConfigItemId, Long prerequisiteConfigItemId);
} 