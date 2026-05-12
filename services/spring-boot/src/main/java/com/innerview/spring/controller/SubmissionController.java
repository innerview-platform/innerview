package com.innerview.spring.controller;

import com.innerview.spring.dto.RunCodeRequest;
import com.innerview.spring.dto.SubmissionAcceptedResponse;
import com.innerview.spring.dto.SubmissionResultDTO;
import com.innerview.spring.dto.SubmitCodeRequest;
import com.innerview.spring.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/sessions/{id}/submissions")
    public ResponseEntity<SubmissionAcceptedResponse> submitCode(
            @PathVariable Long id,
            @AuthenticationPrincipal UUID currentUserId,
            @Valid @RequestBody SubmitCodeRequest request
    ) {
        SubmissionAcceptedResponse response = submissionService.submitCode(id, currentUserId, request);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<SubmissionResultDTO> getSubmissionResult(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return ResponseEntity.ok(submissionService.getSubmissionResult(id, currentUserId));
    }

    @PostMapping("/problems/{id}/run")
    public ResponseEntity<SubmissionResultDTO> runCode(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID currentUserId,
            @Valid @RequestBody RunCodeRequest request
    ) {
        return ResponseEntity.ok(submissionService.runProblem(id, currentUserId, request));
    }
}
