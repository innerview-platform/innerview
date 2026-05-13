package com.innerview.spring.service.impl;

import com.innerview.spring.dto.SignalingMessage;
import com.innerview.spring.entity.ActiveRoom;
import com.innerview.spring.service.WebRtcService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** WebRtcP2pImpl */
@Service
@RequiredArgsConstructor
public class WebRtcP2pImpl implements WebRtcService {
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public void join(ActiveRoom activeRoom, UUID userId) {
    if (activeRoom.getMaxParticipants() == -1) return;
    String roomId = activeRoom.getRoomId();
    int numberOfPartcipants = activeRoom.getActiveParticipants();
    String assignedRole = (numberOfPartcipants == 1) ? "impolite" : "polite";
    Map<String, Object> payload = new HashMap<>();
    payload.put("role", assignedRole);
    payload.put("targetUserId", userId.toString());
    SignalingMessage roleMessage = new SignalingMessage();

    roleMessage.setType("ROLE");
    roleMessage.setPayload(payload);
    System.out.println(roleMessage);
    messagingTemplate.convertAndSend("/topic/room/" + roomId + "/videocall", roleMessage);
  }

  @Override
  public void handleSignal(String roomId, SignalingMessage signalingMessage) {
    messagingTemplate.convertAndSend("/topic/room/" + roomId + "/videocall", signalingMessage);
  }
}
