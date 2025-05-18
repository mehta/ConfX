package com.abhinavmehta.confx.repository;

import com.abhinavmehta.confx.entity.ConfigItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigItemRepository extends JpaRepository<ConfigItem, Long> {
    Optional<ConfigItem> findByProjectIdAndConfigKey(Long projectId, String configKey);
    List<ConfigItem> findByProjectId(Long projectId);
    Optional<ConfigItem> findByIdAndProjectId(Long configItemId, Long projectId);
} 