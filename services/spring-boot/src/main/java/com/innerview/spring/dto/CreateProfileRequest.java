package com.innerview.spring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.innerview.spring.enums.ExperienceLevel;
import com.innerview.spring.enums.InterviewRole;
import lombok.Data;

@Data
public class CreateProfileRequest {

    @JsonProperty("experience_level")
    private ExperienceLevel experienceLevel;

    @JsonProperty("preferred_role")
    private InterviewRole preferredRole;

    @JsonProperty("bio")
    private String bio;

    @JsonProperty("image_url")
    private String imageUrl;
}
