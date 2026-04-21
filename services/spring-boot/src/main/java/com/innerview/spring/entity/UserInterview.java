package com.innerview.spring.entity;

import com.innerview.spring.enums.InterviewRole; // Assuming you have this enum
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_interview")
@Data

public class UserInterview {

    @EmbeddedId
    private UserInterviewId id = new UserInterviewId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("interviewId")
    @JoinColumn(name = "interview_id")
    private Interview interview;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewRole role;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    private Boolean isMuted = false;
}