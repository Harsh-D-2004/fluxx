package com.project.fluxx.FlagManagement.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "flag_event")
public class FlagEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "flag_id", nullable = false)
    private String flagId;

    @JsonProperty("event_type")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", nullable = false, columnDefinition = "json")
    private String afterState;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FlagEvent(String flagId, EventType eventType, String afterState) {
        this.flagId = flagId;
        this.eventType = eventType;
        this.afterState = afterState;
    }

    public enum EventType {
        FLAG_CREATED,
        FLAG_ENABLED,
        FLAG_DISABLED,
        FLAG_ARCHIVED,
        STRATEGY_ADDED,
        STRATEGY_UPDATED,
        STRATEGY_REMOVED
    }
}
