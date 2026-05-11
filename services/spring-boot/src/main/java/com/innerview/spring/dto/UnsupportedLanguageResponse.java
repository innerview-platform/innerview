package com.innerview.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UnsupportedLanguageResponse {
    private String error;
    private List<String> supportedLanguages;
}
