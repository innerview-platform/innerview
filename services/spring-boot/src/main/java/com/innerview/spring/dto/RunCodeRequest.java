package com.innerview.spring.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RunCodeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String language;
}
