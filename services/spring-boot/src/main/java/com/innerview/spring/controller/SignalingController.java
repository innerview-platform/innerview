package com.innerview.spring.controller;

import com.innerview.spring.core.config.StompPrincipal;
import com.innerview.spring.dto.SignalingMessage;
import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.service.RoomService;
import com.innerview.spring.service.SharedCodeEditorService;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SignalingController {

    private final RoomService roomService;
    private final SharedCodeEditorService sharedCodeEditorService; // 1. Inject the code service

    /** Catches ALL real-time WebSocket messages sent to /app/signal.send */
    @MessageMapping("/signal.send")
    public void handleRoomSignals(
            @Payload SignalingMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        String type = message.getType();
        // casting so we can use the method getUserId() and getRoomId()
        StompPrincipal stompPrincipal = (StompPrincipal) principal;
        UUID senderId = stompPrincipal.getUserId();
        String roomId = stompPrincipal.getRoomId();
        // Extract the underlying WebSocket TCP Session ID
        String sessionId = headerAccessor.getSessionId();
        // check of the user joined room before sending messages via stomp

        switch (type) {
            case "JOIN":
                roomService.handleUserConnectedToSocket(roomId, senderId, sessionId);
                break;

            case "JOIN_FEATURE":
                // Payload e.g.: {"element": "SHARED_EDITOR"}
                @SuppressWarnings("unchecked")
                Map<String, Object> featurePayload = (Map<String, Object>) message.getPayload();
                String featureName = (String) featurePayload.get("element");

                roomService.handleJoinFeature(roomId, senderId, featureName);
                break;

            case "ROLE_UPDATE":
                // Payload e.g.: {"targetUserId": "...", "newRole": "INTERVIEWER"}
                @SuppressWarnings("unchecked")
                Map<String, Object> rolePayload = (Map<String, Object>) message.getPayload();
                UUID targetId = UUID.fromString((String) rolePayload.get("targetUserId"));
                InterviewRole newRole = InterviewRole.valueOf((String) rolePayload.get("newRole"));

                roomService.changeParticipantRole(roomId, senderId, targetId, newRole);
                break;

            case "CODE_UPDATE":
                // 2. Handle the live code synchronization
                // Payload e.g.: {"code": "public class Main { ... }"}
                @SuppressWarnings("unchecked")
                Map<String, Object> codePayload = (Map<String, Object>) message.getPayload();
                String newCode = (String) codePayload.get("code");

                sharedCodeEditorService.updateCode(roomId, newCode);
                break;

            case "OFFER":
            case "ANSWER":
            case "ICE_CANDIDATE":
            case "LEAVE":
                // Route WebRTC peer-to-peer data directly to the room topic
                roomService.routeWebRtcSignal(roomId, message);
                break;

            default:
                throw new IllegalArgumentException("Unknown signaling message type: " + type);
        }
    }
}
