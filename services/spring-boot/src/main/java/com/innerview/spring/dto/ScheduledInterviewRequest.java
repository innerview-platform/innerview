package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.enums.RoomSize;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class ScheduledInterviewRequest {
  InterviewType interviewType;
  RoomSize roomSize;
  InterviewRole creatorInterviewRole;
  Instant startTime;
  List<UUID> problemIds;
}
