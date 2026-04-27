package com.innerview.spring.dto;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgrammingLanguageDto {
    private UUID id;
    private String name;
}