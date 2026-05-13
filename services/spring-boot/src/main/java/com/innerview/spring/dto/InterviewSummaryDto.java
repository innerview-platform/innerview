package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import java.time.Instant;

public record InterviewSummaryDto(
    Long id,
    String roomId,
    InterviewType type,
    InterviewStatus status,
    Instant startTime,
    Integer durationMinutes) {}
