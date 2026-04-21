package com.innerview.spring.controller;

import com.innerview.spring.dto.CreateProgrammingLanguageRequest;
import com.innerview.spring.dto.ProgrammingLanguageDto;
import com.innerview.spring.service.UserLanguageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/programming-languages")
public class ProgrammingLanguageController {

    private final UserLanguageService userLanguageService;

    @PostMapping
    public ResponseEntity<ProgrammingLanguageDto> createLanguage(
            @Valid @RequestBody CreateProgrammingLanguageRequest request) {
        ProgrammingLanguageDto created = userLanguageService.createProgrammingLanguage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}