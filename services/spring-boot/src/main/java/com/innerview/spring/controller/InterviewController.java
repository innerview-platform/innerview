package com.innerview.spring.controller;

import com.innerview.spring.dto.InstantInterviewRequest;
import com.innerview.spring.dto.InterviewResponse;
import com.innerview.spring.dto.InterviewSummaryDto;
import com.innerview.spring.dto.ScheduledInterviewRequest;
import com.innerview.spring.service.InterviewService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {
  private final InterviewService interviewService;

  @GetMapping("/user/{userId}/history")
  public ResponseEntity<?> getUserInterviewHistory(@PathVariable UUID userId) {
    List<InterviewSummaryDto> history = interviewService.getInterviewHistory(userId);
    return ResponseEntity.ok(history);
  }

  @GetMapping("/user/createdInterviews")
  public ResponseEntity<?> getUserCreatedInterviews(@AuthenticationPrincipal UUID currentUserId) {
    List<InterviewSummaryDto> createdInterviews =
        interviewService.getCreatedInterview(currentUserId);
    return ResponseEntity.ok(createdInterviews);
  }

  @PatchMapping("/{interviewId}/cancel")
  public ResponseEntity<Void> cancelInterview(
      @PathVariable Long interviewId, @AuthenticationPrincipal UUID currentUserId) {
    interviewService.cancelInterview(interviewId, currentUserId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{interviewId}/complete")
  public ResponseEntity<Void> completeInterview(
      @PathVariable Long interviewId, @AuthenticationPrincipal UUID currentUserId) {
    interviewService.completeInterview(interviewId, currentUserId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/instant")
  public ResponseEntity<?> createInstantInterview(
      @RequestBody InstantInterviewRequest instantInterviewRequest,
      @AuthenticationPrincipal UUID currentUserId) {
    InterviewResponse InterviewResponse =
        interviewService.createInstantInterview(instantInterviewRequest, currentUserId);
    return ResponseEntity.ok(InterviewResponse);
  }

  @PostMapping("/scheduled")
  public ResponseEntity<?> createScheduledInterview(
      @RequestBody ScheduledInterviewRequest scheduledInterviewRequest,
      @AuthenticationPrincipal UUID currentUserId) {

    InterviewResponse interviewResponse =
        interviewService.createScheduledInterview(scheduledInterviewRequest, currentUserId);
    return ResponseEntity.ok(interviewResponse);
  }
}
