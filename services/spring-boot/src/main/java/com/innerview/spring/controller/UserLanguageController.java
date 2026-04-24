package com.innerview.spring.controller;

import com.innerview.spring.dto.*;
import com.innerview.spring.service.UserLanguageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile/languages")
public class UserLanguageController {

    private final UserLanguageService userLanguageService;

    @PostMapping
    public ResponseEntity<MessageResponse> addLanguage(
            @AuthenticationPrincipal UUID currentUserId,
            @Valid @RequestBody AddLanguageRequest request) {
        MessageResponse response = userLanguageService.addLanguage(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{languageId}")
    public ResponseEntity<MessageResponse> removeLanguage(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable UUID languageId) {
        MessageResponse response = userLanguageService.removeLanguage(currentUserId, languageId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProgrammingLanguageDto>> getUserLanguages(
            @AuthenticationPrincipal UUID currentUserId) {
        List<ProgrammingLanguageDto> languages = userLanguageService.getUserLanguages(currentUserId);
        return ResponseEntity.ok(languages);
    }
}