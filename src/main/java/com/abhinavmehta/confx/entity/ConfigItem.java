package com.abhinavmehta.confx.entity;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "config_items", schema = "confx_schema",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "config_key"})
    },
    indexes = {
        @Index(name = "idx_config_items_project_key_entity", columnList = "project_id, config_key")
    }
)
public class ConfigItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private ConfigDataType dataType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Actual values per environment and versioning will be handled by ConfigVersion entity
} 