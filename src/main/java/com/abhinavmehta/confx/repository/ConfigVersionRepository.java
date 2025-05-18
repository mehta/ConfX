package com.abhinavmehta.confx.repository;

import com.abhinavmehta.confx.entity.ConfigVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigVersionRepository extends JpaRepository<ConfigVersion, Long> {

    Optional<ConfigVersion> findByConfigItemIdAndEnvironmentIdAndIsActiveTrue(Long configItemId, Long environmentId);

    List<ConfigVersion> findByConfigItemIdAndEnvironmentIdOrderByVersionNumberDesc(Long configItemId, Long environmentId);

    @Query("SELECT COALESCE(MAX(cv.versionNumber), 0) FROM ConfigVersion cv WHERE cv.configItem.id = :configItemId AND cv.environment.id = :environmentId")
    Integer findMaxVersionNumberByConfigItemAndEnvironment(@Param("configItemId") Long configItemId, @Param("environmentId") Long environmentId);

    @Modifying
    @Query("UPDATE ConfigVersion cv SET cv.isActive = false WHERE cv.configItem.id = :configItemId AND cv.environment.id = :environmentId AND cv.isActive = true")
    void deactivateActiveVersions(@Param("configItemId") Long configItemId, @Param("environmentId") Long environmentId);
    
    Optional<ConfigVersion> findByConfigItemIdAndEnvironmentIdAndVersionNumber(Long configItemId, Long environmentId, Integer versionNumber);

} 