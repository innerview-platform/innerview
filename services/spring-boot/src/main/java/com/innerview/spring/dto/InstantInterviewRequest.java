package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.enums.RoomSize;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class InstantInterviewRequest {
  InterviewType interviewType;
  @NotNull RoomSize roomSize;
  @NotNull InterviewRole creatorInterviewRole;
  Integer durationMinutes;
  List<UUID> problemIds;
}
