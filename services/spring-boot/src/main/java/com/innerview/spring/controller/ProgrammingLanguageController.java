package com.innerview.spring.controller;

import com.innerview.spring.dto.CreateProgrammingLanguageRequest;
import com.innerview.spring.dto.ProgrammingLanguageDto;
import com.innerview.spring.service.UserLanguageService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<ProgrammingLanguageDto>> getLanguages(){
        return ResponseEntity.status(HttpStatus.OK).body(userLanguageService.getAllLanguages());
    }
}