package com.innerview.spring.service.impl;

import com.innerview.spring.dto.SfuAccessTokenDto;
import com.innerview.spring.entity.User;
import com.innerview.spring.exception.UserNotFound;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.SfuService;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SfuServiceImpl implements SfuService {
    private final String API_KEY = "devkey";
    private final String API_SECRET = "secret";
    private final UserRepository userRepository;
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
}
