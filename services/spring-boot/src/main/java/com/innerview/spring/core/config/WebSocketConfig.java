package com.innerview.spring.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // <-- THIS is the magic annotation that creates your missing bean!
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the HTTP endpoint your React frontend will hit to open the WebSocket
        registry.addEndpoint("/ws-signal")
                .setAllowedOriginPatterns("*") // Prevents CORS errors from your frontend
                .withSockJS(); // Optional fallback for older browsers
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // This is the prefix for messages the SERVER broadcasts to the frontend (e.g., /topic/room/123)
        registry.enableSimpleBroker("/topic");

        // This is the prefix for messages the FRONTEND sends to the server (e.g., /app/signal.send)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
