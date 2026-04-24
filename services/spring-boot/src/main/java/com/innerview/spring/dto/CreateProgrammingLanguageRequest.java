package com.innerview.spring.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProgrammingLanguageRequest {

    @NotBlank(message = "Language name must not be blank")
    private String name;
}