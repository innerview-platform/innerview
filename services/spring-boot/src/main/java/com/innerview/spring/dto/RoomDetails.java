package com.innerview.spring.dto;

import com.innerview.spring.enums.RoomSize;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/** RoomDetails */
@Data
public class RoomDetails {
  CodeUpdatePayload codeSnapshot;
  UUID ownerId;
  List<SimplifiedRoomParticipant> participants;
  RoomSize roomSize;
}
