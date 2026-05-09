package com.innerview.spring.service;

import com.innerview.spring.dto.SfuAccessTokenDto;
import com.innerview.spring.entity.User;
import com.innerview.spring.exception.UserNotFound;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public interface SfuService {
     SfuAccessTokenDto generateSfuAccessToken(String roomId, UUID userId);
}
