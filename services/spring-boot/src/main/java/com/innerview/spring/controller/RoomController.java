package com.innerview.spring.controller;

import com.innerview.spring.dto.ActiveRoomDto;
import com.innerview.spring.dto.SfuAccessTokenDto;
import com.innerview.spring.service.RoomService;
import java.util.UUID;

import com.innerview.spring.service.SfuService;
import io.livekit.server.AccessToken;
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
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(
            @PathVariable String roomId, @AuthenticationPrincipal UUID currentUserId) {

        // The service handles DB checks, capacity checks, and RAM allocation
        ActiveRoomDto roomState = roomService.joinRoom(roomId, currentUserId);
        return ResponseEntity.ok().body(roomState);
    }

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

    @CrossOrigin(origins = "*")
    @GetMapping("/token")
    public ResponseEntity<SfuAccessTokenDto> getToken(@AuthenticationPrincipal UUID currentUserId,
                                                      @RequestParam String roomId) {
        return ResponseEntity.ok().body(sfuService.generateSfuAccessToken(roomId,currentUserId));
    }
}

