package com.innerview.spring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmitCodeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String language;

    @NotNull
    private UUID problemId;
}
