package com.innerview.spring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewHistoryDto {

    @JsonProperty("interview_id")
    private Long interviewId;

    private String type;

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("duration_minutes")
    private Integer durationMinutes;

    private String role;
}