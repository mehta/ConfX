package com.abhinavmehta.confx.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "rules", schema = "confx_schema",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"config_version_id", "priority"})
    },
    indexes = {
        @Index(name = "idx_rules_cv_priority_entity", columnList = "config_version_id, priority ASC")
    }
)
public class Rule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_version_id", nullable = false)
    private ConfigVersion configVersion;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "condition_expression", nullable = false, columnDefinition = "TEXT")
    private String conditionExpression;

    @Column(name = "value_to_serve", nullable = false, columnDefinition = "TEXT")
    private String valueToServe; // Must be valid for ConfigItem's DataType

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // createdAt and updatedAt are inherited from BaseEntity
} 