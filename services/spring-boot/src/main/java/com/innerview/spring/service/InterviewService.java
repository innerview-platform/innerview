package com.innerview.spring.service;

import com.innerview.spring.dto.InstantInterviewRequest;
import com.innerview.spring.dto.InterviewResponse;
import com.innerview.spring.dto.InterviewSummaryDto;
import com.innerview.spring.dto.ScheduledInterviewRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface InterviewService {
  public List<InterviewSummaryDto> getInterviewHistory(UUID userId);

  public InterviewResponse createInstantInterview(
      InstantInterviewRequest instantInterviewRequest, UUID currentUserId);

  public InterviewResponse createScheduledInterview(
      ScheduledInterviewRequest scheduledInterviewRequest, UUID currentUserId);
}
