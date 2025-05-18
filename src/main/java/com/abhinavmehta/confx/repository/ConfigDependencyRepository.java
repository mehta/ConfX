package com.abhinavmehta.confx.repository;

import com.abhinavmehta.confx.entity.ConfigDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigDependencyRepository extends JpaRepository<ConfigDependency, Long> {
    List<ConfigDependency> findByDependentConfigItemId(Long dependentConfigItemId);
    List<ConfigDependency> findByPrerequisiteConfigItemId(Long prerequisiteConfigItemId);
    Optional<ConfigDependency> findByDependentConfigItemIdAndPrerequisiteConfigItemId(Long dependentConfigItemId, Long prerequisiteConfigItemId);

    // Fetches dependencies where the DEPENDENT config item belongs to the given project.
    // If prerequisite can be in another project, this is correct.
    // If both dependent and prerequisite must be in the same project, the query needs JOIN and check on prerequisite's project too.
    @Query("SELECT cd FROM ConfigDependency cd WHERE cd.dependentConfigItem.project.id = :projectId")
    List<ConfigDependency> findAllByDependentProject(@Param("projectId") Long projectId);
} 