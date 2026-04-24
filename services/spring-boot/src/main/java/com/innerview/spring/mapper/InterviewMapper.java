package com.innerview.spring.mapper;

import com.innerview.spring.dto.InstantInterviewRequest;
import com.innerview.spring.dto.InterviewSummaryDto;
import com.innerview.spring.entity.Interview;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

  /** Converts a full Interview database entity into a lightweight Summary DTO. */
  public InterviewSummaryDto toSummaryDto(Interview interview) {
    if (interview == null) {
      return null;
    }

    return new InterviewSummaryDto(
        interview.getId(),
        interview.getType(),
        interview.getStatus(),
        interview.getStartTime(),
        interview.getDurationMinutes());
  }

}

