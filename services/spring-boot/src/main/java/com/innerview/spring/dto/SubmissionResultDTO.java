package com.innerview.spring.dto;

import com.innerview.spring.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultDTO {
    private UUID submissionId;
    private Long sessionId;
    private UUID problemId;
    private SubmissionStatus status;
    private Integer score;
    private Long totalDurationMs;
    private List<SubmissionTestCaseResultDTO> testResults = new ArrayList<>();
}
