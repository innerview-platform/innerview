package com.innerview.spring.service.impl;

import com.innerview.spring.dto.CodeUpdatePayload;
import com.innerview.spring.dto.RoomDetails;
import com.innerview.spring.dto.SimplifiedRoomParticipant;
import com.innerview.spring.entity.*;
import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.enums.RoomParticipantStatus;
import com.innerview.spring.enums.RoomSize;
import com.innerview.spring.exception.*;
import com.innerview.spring.repository.InterviewRepository;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.RoomService;
import com.innerview.spring.service.SharedCodeEditorService;
import com.innerview.spring.service.WebRtcService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
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
  public void initRoom(
      Long interviewId, String roomId, UUID ownerId, InterviewType type, Integer roomSize) {
    if (activeRooms.containsKey(roomId)) {
      throw new IllegalStateException("Room with ID: " + roomId + " already exists");
    }

    ActiveRoom room = new ActiveRoom();
    room.setRoomId(roomId);
    room.setInterviewId(interviewId);
    room.setOwnerId(ownerId);
    room.setMaxParticipants(roomSize);
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
  public void joinRoom(String roomId, UUID userId) {
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
          || interviewStatus == InterviewStatus.GHOSTED) {
        throw new ExpiredRoomException("Interview Completed or cancelled");
      }
      if (interviewStatus == InterviewStatus.SCHEDULED
          && Instant.now().isBefore(interview.getStartTime())) {
        throw new RoomNotReadyException("Interview's schedule time has not begun yet");
      }

      initRoom(
          interview.getId(),
          roomId,
          interview.getOwnerId(),
          interview.getType(),
          interview.getRoomSize());
      interview.setStatus(InterviewStatus.STARTED);
      interviewRepository.save(interview);
      room = activeRooms.get(roomId);
    }

    // check if user joined this room before and connected
    RoomParticipant roomParticipant = room.getParticipants().get(userId);
    if (roomParticipant != null && roomParticipant.getStatus() == RoomParticipantStatus.CONNECTED) {
      return;
    }

    // Capacity Check
    if (room.getActiveParticipants() >= room.getMaxParticipants()
        && room.getMaxParticipants() != -1) {
      throw new FullRoomException("Room is full");
    }

    // Preserve ephemeral state (e.g., mute status) on reconnects

    if (roomParticipant == null) {
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
      roomParticipant.setStatus(RoomParticipantStatus.CONNECTED);
    }
    room.incrementActiveParticipants();
    room.setLastActiveAt(Instant.now());
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
      // the user should remove the shared canvas
      // Notify remaining participants so the video grid updates
      Map<String, Object> connectionIssuePayload = new HashMap<>();
      connectionIssuePayload.put("userId", userId.toString());
      messagingTemplate.convertAndSend(
          "/topic/room/" + roomId + "USER_LEFT", connectionIssuePayload);
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
    ActiveRoom room = activeRooms.get(roomId);
    UUID userId = disconnectedClient.getUserId();

    if (room.getUiConfig().isShowSharedEditor()) {
      sharedCodeEditorService.removeUserFromSession(userId, roomId);
    }
    // preparing the message
    Map<String, Object> connectionIssuePayload = new HashMap<>();
    connectionIssuePayload.put("userId", userId.toString());
    messagingTemplate.convertAndSend(
        "/topic/room/" + roomId + "USER_DISCONNECTED", connectionIssuePayload);
    activeRooms
        .get(roomId)
        .getParticipants()
        .get(userId)
        .setStatus(RoomParticipantStatus.DISCONNECTED);
    activeRooms.get(roomId).decrementActiveParticipants();
    activeRooms.get(roomId).setLastActiveAt(Instant.now()); // Mark for cleanup
  }

  public boolean hasUserJoinedRoom(String roomId, UUID userId) {
    ActiveRoom room = activeRooms.get(roomId);
    if (room == null) return false;
    if (room.getParticipants().get(userId) == null) return false;
    return true;
  }

  @Override
  public boolean isRoomExists(String roomId) {
    return activeRooms.containsKey(roomId);
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
      // join p2p
      if (room.getMaxParticipants() == 2) {
        webRtcService.join(room, userId);
      }

      // join shared code editor
      if (room.getUiConfig().isShowSharedEditor()) {
        sharedCodeEditorService.addUserToSession(userId, roomId);
      }
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

      // Broadcast role change
      messagingTemplate.convertAndSend(
          "/topic/room/" + roomId + "/roles", Map.of("userId", targetUserId, "newRole", newRole));
    }
  }

  @Override
  public RoomDetails getRoomDetails(UUID requesterId, String roomId) {
    ActiveRoom activeRoom = activeRooms.get(roomId);
    if (activeRoom == null) throw new ResourceNotFoundException("Room can't be found");

    RoomParticipant roomParticipant = activeRoom.getParticipants().get(requesterId);
    if (roomParticipant == null) throw new AccessDeniedException("You should join room first");

    Set<UUID> userIds = activeRoom.getParticipants().keySet();

    // 2. Fetch all users from the database in ONE query
    Map<UUID, String> userNamesMap =
        userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, User::getName));

    List<SimplifiedRoomParticipant> participantDtos =
        activeRoom.getParticipants().values().stream()
            .map(
                p -> {
                  String name = userNamesMap.getOrDefault(p.getUserId(), "Unknown");
                  return new SimplifiedRoomParticipant(p.getUserId(), name, p.getRole());
                })
            .toList();

    CodeUpdatePayload currentCode = sharedCodeEditorService.getCodeSnapshot(roomId);
    RoomSize roomSize =
        (activeRoom.getMaxParticipants() == 2) ? RoomSize.ONE_ON_ONE : RoomSize.MANY;

    // 4. Fill and return the DTO
    RoomDetails roomDetails = new RoomDetails();
    roomDetails.setOwnerId(activeRoom.getOwnerId());
    roomDetails.setCodeSnapshot(currentCode);
    roomDetails.setRoomSize(roomSize);
    roomDetails.setParticipants(participantDtos);

    return roomDetails;
  }

  // ==========================================
  // BACKGROUND TASKS
  // ==========================================

  @Scheduled(fixedDelay = 300000) // Runs every 5 minutes
  public void cleanupEmptyRooms() {
    Instant now = Instant.now();

    // Create an iterator for the map's entries
    var iterator = activeRooms.entrySet().iterator();

    while (iterator.hasNext()) {
      var entry = iterator.next();
      String roomId = entry.getKey();
      ActiveRoom room = entry.getValue();

      // 10-minute empty condition
      if (room.getActiveParticipants() == 0
          && room.getLastActiveAt().plusSeconds(600).isBefore(now)) {
        Interview interview =
            interviewRepository
                .findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        interview.setStatus(InterviewStatus.COMPLETED);
        interviewRepository.save(interview);
      }
    }
  }
}
