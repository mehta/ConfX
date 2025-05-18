package com.abhinavmehta.confx.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "config_versions", schema = "confx_schema",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"config_item_id", "environment_id", "version_number"})
    },
    indexes = {
        @Index(name = "idx_cv_item_env_active_entity", columnList = "config_item_id, environment_id, isActive") // Consider DB index `WHERE isActive = TRUE` for more specific optimization if JPA supports it or rely on DB index.
    }
)
public class ConfigVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_item_id", nullable = false)
    private ConfigItem configItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value; // Stored as string, actual type interpretation based on ConfigItem.dataType

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    // createdAt and updatedAt are inherited from BaseEntity
} 