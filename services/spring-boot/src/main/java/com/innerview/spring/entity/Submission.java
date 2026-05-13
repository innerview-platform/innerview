package com.innerview.spring.entity;

import com.innerview.spring.dto.SubmissionTestResult;
import com.innerview.spring.enums.SubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "submissions",
        indexes = {
                @Index(name = "idx_submissions_interview_problem", columnList = "interview_id, problem_id"),
                @Index(name = "idx_submissions_user_interview", columnList = "user_id, interview_id")
        }
)
@Check(name = "ck_submissions_score_range", constraints = "score between 0 and 100")
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @NotNull
    @Column(name = "interview_id", nullable = false, updatable = false)
    private Long interviewId;

    @NotNull
    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    @NotBlank
    @Column(name = "code", nullable = false, columnDefinition = "TEXT")
    private String code;

    @NotBlank
    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "test_results", nullable = false, columnDefinition = "json")
    private List<SubmissionTestResult> testResults = new ArrayList<>();

    @Column(name = "compilation_error", columnDefinition = "TEXT")
    private String compilationError;

    @NotNull
    @Min(0)
    @Column(name = "total_duration_ms", nullable = false)
    private Long totalDurationMs = 0L;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_submissions_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "interview_id",
            nullable = false,
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_submissions_interview")
    )
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "problem_id",
            nullable = false,
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_submissions_problem")
    )
    private Problem problem;
}
