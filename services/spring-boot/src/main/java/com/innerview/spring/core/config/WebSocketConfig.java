package com.innerview.spring.core.config;

import com.innerview.spring.core.util.JwtUtil;
import com.innerview.spring.service.RoomService;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtUtil jwtUtil;
    private final RoomService roomService;

    public WebSocketConfig(JwtUtil jwtUtil,@Lazy RoomService roomService) {
        this.jwtUtil = jwtUtil;
        this.roomService = roomService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the HTTP endpoint your React frontend will hit to open the WebSocket
        registry
                .addEndpoint("/ws-signal")
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

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                new ChannelInterceptor() {
                    @Override
                    public Message<?> preSend(Message<?> message, MessageChannel channel) {
                        StompHeaderAccessor accessor =
                                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                            String bearerToken = accessor.getFirstNativeHeader("Authorization");
                            String roomId = accessor.getFirstNativeHeader("roomId");

                            UUID userId = null;
                            // Validating access token and extracting the userId
                            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                                String jwt = bearerToken.substring(7);
                                if (jwtUtil.validateToken(jwt)) {
                                    userId = jwtUtil.extractUserId(jwt);
                                }
                            }
                            if (userId != null) {
                                roomService.joinRoom(roomId, userId);
                                accessor.setUser(new StompPrincipal(userId, roomId));
                                roomService.mapSessionIdToUser(accessor.getSessionId(), roomId, userId);
                                return message;
                            }
                            throw new MessagingException("unauthorized");
                        }

                        return message;
                    }
                });
    }
}
