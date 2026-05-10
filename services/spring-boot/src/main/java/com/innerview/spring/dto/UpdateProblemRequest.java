package com.innerview.spring.dto;

import com.innerview.spring.entity.ProgrammingLanguage;
import com.innerview.spring.enums.Difficulty;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProblemRequest {
    private String title;

    private String slug;

    private String statement;

    private Difficulty difficulty;

    private List<String> tags;

    private String explanation;

    @Positive
    private Integer timeLimitMs;

    @Positive
    private Integer memoryLimitMb;

    private String solutionCode;

    private ProgrammingLanguage solutionLanguage;

    Boolean isActive;
}
