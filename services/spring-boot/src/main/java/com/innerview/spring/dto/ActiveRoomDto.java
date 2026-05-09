package com.innerview.spring.dto;

import com.innerview.spring.entity.RoomParticipant;
import com.innerview.spring.entity.RoomUiConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Data
public class ActiveRoomDto {
    private String roomId;
    private RoomUiConfig uiConfig;
    private ConcurrentHashMap<UUID, RoomParticipant> participants;
}
