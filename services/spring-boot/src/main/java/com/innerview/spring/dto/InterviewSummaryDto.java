package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import java.time.LocalDateTime;

public record InterviewSummaryDto(
        Long id,
        InterviewType type,
        InterviewStatus status,
        LocalDateTime startTime,
        Integer durationMinutes
) {}
