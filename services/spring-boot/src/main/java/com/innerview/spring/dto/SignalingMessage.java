package com.innerview.spring.dto;

import lombok.Data;

@Data
public class SignalingMessage {
    private String type;      // The routing instruction (e.g., "JOIN", "OFFER")
    private String roomId;    // The 6-character room ID
    private String senderId;  // The UUID of the user sending the message
    private Object payload;   // The flexible data (parsed as a Map in the controller)
}
