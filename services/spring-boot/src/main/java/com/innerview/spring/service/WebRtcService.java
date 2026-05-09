package com.innerview.spring.service;

import com.innerview.spring.dto.SignalingMessage;
import com.innerview.spring.entity.ActiveRoom;
import java.util.UUID;

public interface WebRtcService {
  void join(ActiveRoom activeRoom, UUID userId);

  void handleSignal(String roomId, SignalingMessage signalingMessage);
}
