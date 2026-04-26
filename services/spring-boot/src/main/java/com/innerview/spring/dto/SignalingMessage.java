package com.innerview.spring.dto;

import lombok.Data;

@Data
public class SignalingMessage {
    private String type;      // The routing instruction (e.g., "JOIN", "OFFER")
    private Object payload;   // The flexible data (parsed as a Map in the controller)
}
