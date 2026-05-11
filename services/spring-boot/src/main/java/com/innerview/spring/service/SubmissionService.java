package com.innerview.spring.service;

import com.innerview.spring.dto.RunCodeRequest;
import com.innerview.spring.dto.SubmissionAcceptedResponse;
import com.innerview.spring.dto.SubmissionResultDTO;
import com.innerview.spring.dto.SubmitCodeRequest;

import java.util.UUID;

public interface SubmissionService {

    SubmissionAcceptedResponse submitCode(Long sessionId, UUID currentUserId, SubmitCodeRequest request);

    SubmissionResultDTO getSubmissionResult(UUID submissionId, UUID currentUserId);

    SubmissionResultDTO runProblem(UUID problemId, UUID currentUserId, RunCodeRequest request);
}
