package com.innerview.spring.service;

import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewType;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public interface RoomService {
  void initRoom(
      Long interviewId, String roomId, UUID ownerId, InterviewType type, Integer roomSize);

  void joinRoom(String roomId, UUID userId);

  void leaveRoom(String roomId, UUID userId);

  void handleDisconnect(String sessionId);

  void handleUserConnectedToSocket(String roomId, UUID userId, String stompSessionId);

  void handleJoinFeature(String roomId, UUID userId, String featureName);

  void mapSessionIdToUser(String sessionId, String roomId, UUID userId);

  void changeParticipantRole(
      String roomId, UUID requesterId, UUID targetUserId, InterviewRole newRole);

  boolean hasUserJoinedRoom(String roomId, UUID userId);

  boolean isRoomExists(String roomId);
}
