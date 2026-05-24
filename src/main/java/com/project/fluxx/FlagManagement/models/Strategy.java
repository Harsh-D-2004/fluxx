package com.project.fluxx.FlagManagement.models;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "activation_strategy")
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "flag_id", nullable = false)
    private String flagId;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false, length = 30)
    private StrategyType strategyType;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "strategy_id", nullable = false)
    private List<StrategyParam> parameters;

    public enum StrategyType {
        DEFAULT, ROLLOUT, USER_ID
    }
}
