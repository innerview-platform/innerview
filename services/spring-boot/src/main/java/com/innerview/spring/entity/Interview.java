package com.innerview.spring.entity;

import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Set of interviews associated with this user.
 * * <p><b>Index Strategy:</b></p>
 * <ul>
 * <li><b>Clustered Index:</b> (user_id, interview_id). This optimizes for
 * user-centric queries like "Count all interviews for User X".</li>
 * <li><b>Secondary Index:</b> (interview_id). This ensures that reverse
 * lookups (finding all participants in a specific session) are O(log N).</li>
 * </ul>
 */
@Entity
@Table(name = "interviews")
@Data
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    @ManyToMany(mappedBy = "interviews")
    private List<User> participants;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private UUID ownerId;
    private String roomId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}