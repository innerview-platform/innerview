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
public class FeedbackDto {

    private Integer rating;

    private String comment;

    @JsonProperty("reviewer_id")
    private UUID reviewerId;

    @JsonProperty("interview_id")
    private Long interviewId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}