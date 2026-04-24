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

    public Interview toInterview(InstantInterviewRequest request) {
        if (request == null) {
            return null;
        }

        Interview interview = new Interview();
        interview.setType(request.getInterviewType());
        interview.setStatus(null); // Status will be set when the interview is created
        interview.setStartTime(null); // Start time will be set when the interview is scheduled
        interview.setDurationMinutes(request.getDurationMinutes());
        // Set other fields as necessary

        return interview;
    }
}
