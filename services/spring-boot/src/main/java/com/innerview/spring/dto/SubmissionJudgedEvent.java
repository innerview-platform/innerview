package com.innerview.spring.dto;

import com.innerview.spring.enums.SubmissionStatus;

import java.util.UUID;

public record SubmissionJudgedEvent(
        UUID submissionId,
        Long sessionId,
        Integer score,
        SubmissionStatus status
) {
}
