package com.innerview.spring.controller;

import com.innerview.spring.dto.RoomDetails;
import com.innerview.spring.dto.SfuAccessTokenDto;
import com.innerview.spring.service.RoomService;
import com.innerview.spring.service.SfuService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;
  private final SfuService sfuService;

  /**
   * Called by the frontend via standard HTTP POST right BEFORE opening the WebSocket. Validates the
   * user, initializes the room if necessary, and returns the current state.
   */

  /**
   * Optional endpoint for graceful HTTP-based exits. Useful if the frontend wants to trigger a
   * leave event before tearing down the STOMP client.
   */
  @PostMapping("/{roomId}/leave")
  public ResponseEntity<Void> leaveRoom(
      @PathVariable String roomId, @AuthenticationPrincipal UUID currentUserId) {

    roomService.leaveRoom(roomId, currentUserId);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/{roomId}/roomDetails")
  public ResponseEntity<RoomDetails> getRoomDetails(
      @PathVariable String roomId, @AuthenticationPrincipal UUID currentUserId) {

    roomService.getRoomDetails(currentUserId, roomId);

    return ResponseEntity.ok().build();
  }

  @CrossOrigin(origins = "*")
  @GetMapping("/{roomId}/token")
  public ResponseEntity<SfuAccessTokenDto> getToken(
      @AuthenticationPrincipal UUID currentUserId, @PathVariable String roomId) {
    return ResponseEntity.ok().body(sfuService.generateSfuAccessToken(roomId, currentUserId));
  }
}
