package com.abhinavmehta.confx.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "config_dependencies", schema = "confx_schema",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"dependent_config_item_id", "prerequisite_config_item_id"})
    }
    // The CHECK constraint for non-self dependency is handled at DB level.
)
public class ConfigDependency extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_config_item_id", nullable = false)
    private ConfigItem dependentConfigItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prerequisite_config_item_id", nullable = false)
    private ConfigItem prerequisiteConfigItem;

    @Column(name = "prerequisite_expected_value", nullable = false, columnDefinition = "TEXT")
    private String prerequisiteExpectedValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
} 