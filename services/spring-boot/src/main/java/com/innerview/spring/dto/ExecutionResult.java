package com.innerview.spring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionResult {
    ExecutionOutcome outcome;
    String actualOutput;
    String errorOutput;
    Long durationMs;
    Long memoryBytes;
}
