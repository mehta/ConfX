package com.abhinavmehta.confx.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "environments", schema = "confx_schema",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "name"})
    }
)
public class Environment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "color_tag")
    private String colorTag; // For UI indication, e.g., a hex color code

    // createdAt and updatedAt are inherited from BaseEntity
} 