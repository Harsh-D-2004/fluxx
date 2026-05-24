
package com.project.fluxx.FlagManagement.models;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(nullable = false, length = 100)
    private String name;

    @JsonProperty("flag_type")
    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 20)
    private FlagType flagType;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean enabled = false;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum FlagType {
        KILL_SWITCH, ROLLOUT, TARGETED
    }
}
