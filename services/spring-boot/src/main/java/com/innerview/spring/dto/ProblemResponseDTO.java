package com.innerview.spring.dto;

import com.innerview.spring.enums.Difficulty;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemResponseDTO {
    private UUID id;

    private String title;

    private String slug;

    private String statement;

    private Difficulty difficulty;

    private List<String> tags;


    @Positive
    private Integer timeLimitMs;

    @Positive
    private Integer memoryLimitMb;

    private boolean      isActive;
    private Instant      createdAt;
    private Instant updatedAt;
    private ProblemCreatorDTO   createdBy;
}
