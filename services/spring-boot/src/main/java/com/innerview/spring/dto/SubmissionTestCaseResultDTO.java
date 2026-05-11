package com.innerview.spring.dto;

import com.innerview.spring.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmissionTestCaseResultDTO {
    private Integer testIndex;
    private SubmissionStatus status;
    private Long durationMs;
}
