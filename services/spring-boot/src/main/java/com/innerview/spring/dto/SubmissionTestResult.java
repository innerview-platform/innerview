package com.innerview.spring.dto;

import com.innerview.spring.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionTestResult {
    private UUID testCaseId;
    private SubmissionStatus status;
    private String actualOutput;
    private String errorOutput;
    private Long durationMs;
    private Long memoryBytes;
    private Integer orderIndex;
    private Integer weight;
    private Boolean sample;
}
