package com.aura.auraid.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "approval_steps")
public class ApprovalStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @Column(nullable = false)
    private int stepOrder;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "approval_threshold")
    private Integer approvalThreshold; // Percentage needed for PERCENTAGE_APPROVAL type

    @ManyToMany
    @JoinTable(
        name = "approval_step_approvers",
        joinColumns = @JoinColumn(name = "step_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> approvers;

    @Column(nullable = false)
    private boolean active = true;
} 