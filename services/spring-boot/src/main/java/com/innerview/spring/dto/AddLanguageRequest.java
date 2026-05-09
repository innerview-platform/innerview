package com.innerview.spring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddLanguageRequest {

    @NotNull(message = "language_id must not be null")
    @JsonProperty("language_id")
    private UUID languageId;
}