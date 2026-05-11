package com.innerview.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class SubmissionAcceptedResponse {
    private UUID submissionId;
}
