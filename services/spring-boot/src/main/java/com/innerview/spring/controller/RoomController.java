package com.innerview.spring.controller;

import com.innerview.spring.dto.ActiveRoomDto;
import com.innerview.spring.dto.ErrorMessageResponse;
import com.innerview.spring.exception.FullRoomException;
import com.innerview.spring.exception.RoomNotFoundException;
import com.innerview.spring.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * Called by the frontend via standard HTTP POST right BEFORE opening the WebSocket.
     * Validates the user, initializes the room if necessary, and returns the current state.
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal UUID currentUserId
    ) {

        // The service handles DB checks, capacity checks, and RAM allocation
        try {
            ActiveRoomDto roomState = roomService.joinRoom(roomId, currentUserId);
            return ResponseEntity.ok().body(roomState);
        } catch (FullRoomException e) {
            return ResponseEntity.status(403).body(new ErrorMessageResponse(e.getMessage())); // 403 Forbidden if room is full
        } catch (RoomNotFoundException e) {
            return ResponseEntity.status(404).body(new ErrorMessageResponse(e.getMessage())); // 404 Not Found if room doesn't exist
        }
    }

    /**
     * Optional endpoint for graceful HTTP-based exits.
     * Useful if the frontend wants to trigger a leave event before tearing down the STOMP client.
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal UUID currentUserId) {

        roomService.leaveRoom(roomId, currentUserId);

        return ResponseEntity.ok().build();
    }
}