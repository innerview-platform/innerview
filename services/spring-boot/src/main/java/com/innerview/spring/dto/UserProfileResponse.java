package com.innerview.spring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("experience_level")
    private String experienceLevel;

    @JsonProperty("preferred_role")
    private String preferredRole;

    private String bio;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}