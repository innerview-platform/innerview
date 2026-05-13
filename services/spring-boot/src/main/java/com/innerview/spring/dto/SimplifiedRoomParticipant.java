package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewRole;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SimplifiedRoomParticipant */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimplifiedRoomParticipant {

  private UUID userId;
  private String name;
  private InterviewRole role;
}
