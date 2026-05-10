package com.innerview.spring.entity;

import com.innerview.spring.enums.Difficulty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "problems",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_problems_slug", columnNames = "slug")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String slug;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "problem_tags", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "tag", nullable = false, length = 100)
    private List<String> tags = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Positive
    @Column(name = "time_limit_ms", nullable = false)
    private Integer timeLimitMs = 2000;

    @Positive
    @Column(name = "memory_limit_mb", nullable = false)
    private Integer memoryLimitMb = 256;

    @Column(name = "solution_code", columnDefinition = "TEXT")
    private String solutionCode;

    // LAZY fetch added here
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "solution_language",
            nullable = false,
            updatable = false,
            referencedColumnName = "id"
    )
    private ProgrammingLanguage solutionLanguage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // LAZY fetch maintained here
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            updatable = false,
            referencedColumnName = "id"
    )
    private User createdBy;


    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}