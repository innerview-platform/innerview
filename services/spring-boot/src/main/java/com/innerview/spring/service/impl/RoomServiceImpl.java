package com.innerview.spring.service.impl;

import com.innerview.spring.dto.ActiveRoomDto;
import com.innerview.spring.dto.CodeUpdatePayload;
import com.innerview.spring.dto.SfuAccessTokenDto;
import com.innerview.spring.dto.SignalingMessage;
import com.innerview.spring.entity.*;
import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.enums.RoomParticipantStatus;
import com.innerview.spring.exception.*;
import com.innerview.spring.repository.InterviewRepository;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.RoomService;
import com.innerview.spring.service.SharedCodeEditorService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.innerview.spring.service.WebRtcService;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RoomServiceImpl implements RoomService {

  private final Map<String, ActiveRoom> activeRooms = new ConcurrentHashMap<>();
  private final Map<String, RoomParticipant> sessionDict = new ConcurrentHashMap<>();
  private final SharedCodeEditorService sharedCodeEditorService;
  private final SimpMessagingTemplate messagingTemplate;
  private final InterviewRepository interviewRepository;
  private final WebRtcService webRtcService;
  private final UserRepository userRepository;
  private final String API_KEY = "devkey";
  private final String API_SECRET = "secret";

  //    private final InterviewParticipantRepository participantRepository; // Added for role
  // updates

  // ==========================================
  // REST API METHODS (Room Initialization)
  // ==========================================
  @Override
  public void mapSessionIdToUser(String sessionId, String roomId, UUID userId) {
    RoomParticipant joindUser = activeRooms.get(roomId).getParticipants().get(userId);
    joindUser.setSessionId(sessionId);
    sessionDict.putIfAbsent(sessionId, joindUser);
  }

  @Override
  public void initRoom(Long interviewId, String roomId, UUID ownerId, InterviewType type) {
    if (activeRooms.containsKey(roomId)) {
      throw new IllegalStateException("Room with ID: " + roomId + " already exists");
    }

    ActiveRoom room = new ActiveRoom();
    room.setRoomId(roomId);
    room.setInterviewId(interviewId);
    room.setOwnerId(ownerId);

    // 1. Generate the config based on the exact interview type
    RoomUiConfig initialConfig = RoomUiConfig.defaultForType(type);
    room.setUiConfig(initialConfig);

    room.setCreatedAt(Instant.now());
    room.setLastActiveAt(Instant.now());

    // 2. PRE-WARM REQUIRED SERVICES
    // Dynamically spin up the backend engines based on whatever the UI config demands.

    // Engine A: The Code Editor
    if (initialConfig.isShowSharedEditor()) {
      sharedCodeEditorService.init(roomId);
    }

    // Engine B: The Shared Canvas (e.g., for System Design)
    if (initialConfig.isShowSystemCanvas()) {
      // Uncomment and inject your Canvas service once you build the tldraw integration
      // sharedCanvasService.init(roomId);
    }

    // Engine C: The Problem Statement (e.g., Fetching a random algorithm question)
    if (initialConfig.isShowProblemStatement()) {
      // If your problem statement requires backend initialization (like picking a question
      // from a database or loading markdown), trigger it here.
      // problemManagementService.init(roomId);
    }

    // 3. Save the fully warmed-up room to RAM
    activeRooms.put(roomId, room);
  }

    @Override
    public ActiveRoomDto joinRoom(String roomId, UUID userId) {
        ActiveRoom room = activeRooms.get(roomId);

        // Fallback: Load from DB if server restarted or room dropped from memory
        if (room == null) {
            Interview interview = interviewRepository.getInterviewsByRoomId(roomId);
            if (interview == null) {
                throw new RoomNotFoundException("Room with ID: " + roomId + " not found");
            }

            InterviewStatus interviewStatus = interview.getStatus();
            if (interviewStatus == InterviewStatus.CANCELLED
                    || interviewStatus == InterviewStatus.COMPLETED
                    || (interviewStatus == InterviewStatus.SCHEDULED
                    && Instant.now().isAfter(interview.getEndTime()))) {
                throw new ExpiredRoomException("Interview Completed or cancelled");
            }
            if (interviewStatus == InterviewStatus.SCHEDULED
                    && Instant.now().isBefore(interview.getStartTime())) {
                throw new RoomNotReadyException("Interview's schedule time has not begun yet");
            }

      initRoom(interview.getId(), roomId, interview.getOwnerId(), interview.getType());
      interview.setStatus(InterviewStatus.STARTED);
      interviewRepository.save(interview);
      room = activeRooms.get(roomId);
    }

    // Capacity Check
    if (room.getParticipants().size() >= room.getMaxParticipants()
        && !room.getParticipants().containsKey(userId)) {
      throw new FullRoomException("Room is full");
    }

    // Preserve ephemeral state (e.g., mute status) on reconnects

        RoomParticipant roomParticipant;
        if (!room.getParticipants().containsKey(userId)) {
            roomParticipant = new RoomParticipant();
            roomParticipant.setUserId(userId);
            roomParticipant.setRoomId(roomId);
            roomParticipant.setJoinedAt(Instant.now());
            roomParticipant.setStatus(RoomParticipantStatus.CONNECTED);
            if (userId.equals(room.getOwnerId())) {
                roomParticipant.setRole(InterviewRole.INTERVIEWER);
            } else {
                roomParticipant.setRole(InterviewRole.INTERVIEWEE);
            }
            room.getParticipants().put(roomParticipant.getUserId(), roomParticipant);
        } else {
            room.getParticipants().get(userId).setStatus(RoomParticipantStatus.CONNECTED);
            roomParticipant = room.getParticipants().get(userId);
        }

        room.setLastActiveAt(Instant.now());
        return new ActiveRoomDto(room.getRoomId(), room.getUiConfig(), room.getParticipants());
    }

    @Override
    public void leaveRoom(String roomId, UUID userId) {
        ActiveRoom room = activeRooms.get(roomId);
        if (room == null) return;

        RoomParticipant removed = room.getParticipants().remove(userId);
        if (removed != null) {

      String sessionId = removed.getSessionId();
      sessionDict.remove(sessionId);
      if (room.getUiConfig().isShowSharedEditor()) {
          sharedCodeEditorService.removeUserFromSession(userId, roomId);
      }
      //the user should remove the shared canvas
      // Notify remaining participants so the video grid updates
      Map<String, Object> connectionIssuePayload = new HashMap<>();
      connectionIssuePayload.put("type", "USER_DISCONNECTED");
      connectionIssuePayload.put("userId", userId.toString());
      messagingTemplate.convertAndSend("/topic/room/" + roomId, connectionIssuePayload);
      room.decrementActiveParticipants();
      if (room.getParticipants().isEmpty()) {
        room.setLastActiveAt(Instant.now()); // Mark for cleanup
      }
    }
  }

  @Override
  public void handleDisconnect(String sessionId) {
    RoomParticipant disconnectedClient = sessionDict.get(sessionId);
    if (disconnectedClient == null) return;
    String roomId = disconnectedClient.getRoomId();
    UUID userId = disconnectedClient.getUserId();
    // preparing the message
    Map<String, Object> connectionIssuePayload = new HashMap<>();
    SignalingMessage signalingMessage = new SignalingMessage();
    signalingMessage.setType("USER_DISCONNECTED");
    connectionIssuePayload.put("userId", userId.toString());
    messagingTemplate.convertAndSend("/topic/room/" + roomId, connectionIssuePayload);
    activeRooms
        .get(roomId)
        .getParticipants()
        .get(userId)
        .setStatus(RoomParticipantStatus.DISCONNECTED);
    activeRooms.get(roomId).decrementActiveParticipants();
  }

  public boolean hasUserJoinedRoom(String roomId, UUID userId) {
    ActiveRoom room = activeRooms.get(roomId);
    if (room == null) return false;
    if (room.getParticipants().get(userId) == null) return false;
    return true;
  }

    @Override
    public SfuAccessTokenDto generateSfuAccessToken(String roomId, UUID userId) {
        AccessToken token = new AccessToken(API_KEY, API_SECRET);

        Optional<User> user = userRepository.findUserById(userId);
        if(user.isEmpty())
            throw new UserNotFound("User not found with id: " + userId);
        String participantName = user.get().getName();
        // The display name shown in the UI
        token.setName(participantName);

        // LiveKit REQUIRES a unique identity for every user
        String uniqueIdentity = participantName + "-" + UUID.randomUUID().toString().substring(0, 6);
        token.setIdentity(uniqueIdentity);

        // Grant permission to join the specific room
        token.addGrants(new RoomJoin(true), new RoomName(roomId));

        // Generate and return the perfectly formatted JWT string
        return new SfuAccessTokenDto(token.toJwt());
    }

    // ==========================================
  // STOMP WEBSOCKET METHODS (Live Session)
  // ==========================================

  @Override
  public void handleUserConnectedToSocket(String roomId, UUID userId, String stompSessionId) {
    ActiveRoom room = activeRooms.get(roomId);
    if (room != null && room.getParticipants().containsKey(userId)) {
      // Save STOMP session ID to handle accidental disconnects later
      room.getParticipants().get(userId).setSessionId(stompSessionId);
      room.incrementActiveParticipants();
      webRtcService.join(room, userId);
    }
  }

  @Override
  public void handleJoinFeature(String roomId, UUID userId, String featureName) {
    ActiveRoom room = activeRooms.get(roomId);
    if (room == null) return;

    if ("SHARED_EDITOR".equals(featureName)) {
      // 1. Initialize room-wide feature if it's currently off
      if (!room.getUiConfig().isShowSharedEditor()) {
        room.getUiConfig().setShowSharedEditor(true);
        sharedCodeEditorService.init(roomId);

        // Silently notify the room that the editor is available
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomId + "/ui-available", "SHARED_EDITOR");
      }

      // 2. Add specific user to the session
      sharedCodeEditorService.addUserToSession(userId, roomId);

      // 3. Send the current code snapshot ONLY to the user who requested it
      CodeUpdatePayload currentCode = sharedCodeEditorService.getCodeSnapshot(roomId);
//      messagingTemplate.convertAndSendToUser(
//          userId.toString(), "/queue/editor-snapshot", currentCode);
      messagingTemplate.convertAndSend(
              "/topic/room/" + roomId + "/code", currentCode);
    }
  }

  @Override
  public void changeParticipantRole(
      String roomId, UUID requesterId, UUID targetUserId, InterviewRole newRole) {
    ActiveRoom room = activeRooms.get(roomId);
    if (room == null) throw new RoomNotFoundException("Room not found");

    // Verify permissions
    if (!room.getOwnerId().equals(requesterId)) {
      throw new SecurityException("Only the room owner can change roles.");
    }

    RoomParticipant targetParticipant = room.getParticipants().get(targetUserId);
    if (targetParticipant != null) {
      // Update RAM
      targetParticipant.setRole(newRole);

      // Update Database
      //            participantRepository.updateRole(room.getInterviewId(), targetUserId, newRole);

      // Broadcast role change
      messagingTemplate.convertAndSend(
          "/topic/room/" + roomId + "/roles", Map.of("userId", targetUserId, "newRole", newRole));
    }
  }


  // ==========================================
  // BACKGROUND TASKS
  // ==========================================

  @Scheduled(fixedDelay = 300000) // Runs every 5 minutes
  public void cleanupEmptyRooms() {
    Instant now = Instant.now();
    // Remove rooms that have been empty for more than 10 minutes
    activeRooms
        .entrySet()
        .removeIf(
            entry ->
                entry.getValue().getParticipants().isEmpty()
                    && entry.getValue().getLastActiveAt().plusSeconds(600).isBefore(now));
  }
}
