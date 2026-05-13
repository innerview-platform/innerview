package com.innerview.spring.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class TestCaseDto {
    private UUID id;             // Added ID for frontend use
    private String input;
    private String expectedOutput;
    private boolean isSample;
    private Integer orderIndex;
    private String description;
    private Integer weight;
}