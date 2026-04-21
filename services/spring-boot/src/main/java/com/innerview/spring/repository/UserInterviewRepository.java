package com.innerview.spring.repository;

import com.innerview.spring.dto.InterviewHistoryDto;
import com.innerview.spring.entity.UserInterview;
import com.innerview.spring.entity.UserInterviewId;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserInterviewRepository extends JpaRepository<UserInterview, UserInterviewId> {
    @Query("""
        SELECT new com.innerview.spring.dto.InterviewHistoryDto(
            i.id, cast(i.type as string), i.startTime, i.durationMinutes, cast(ui.role as string)
        )
        FROM UserInterview ui
        JOIN ui.interview i
        WHERE ui.user.id = :userId
        AND (:status IS NULL OR i.status = :status)
        AND (:type IS NULL OR i.type = :type)
    """)
    Page<InterviewHistoryDto> findInterviewHistoryByUser(
            @Param("userId") UUID userId,
            @Param("status") InterviewStatus status,
            @Param("type") InterviewType type,
            Pageable pageable
    );
}
