package com.innerview.spring.service;

import com.innerview.spring.dto.InstantInterviewRequest;
import com.innerview.spring.dto.InterviewResponse;
import com.innerview.spring.dto.InterviewSummaryDto;
import com.innerview.spring.dto.ScheduledInterviewRequest;
import com.innerview.spring.entity.Interview;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface InterviewService {
    public List<InterviewSummaryDto> getInterviewHistory(Long userId);
    public InterviewResponse createInstantInterview(InstantInterviewRequest instantInterviewRequest);
    public InterviewResponse createScheduledInterview(ScheduledInterviewRequest scheduledInterviewRequest);
}
