package com.innerview.spring.service;

import com.innerview.spring.dto.*;

import java.util.List;
import java.util.UUID;

public interface UserLanguageService {

    MessageResponse addLanguage(UUID userId, AddLanguageRequest request);

    MessageResponse removeLanguage(UUID userId, UUID languageId);

    List<ProgrammingLanguageDto> getUserLanguages(UUID userId);

    ProgrammingLanguageDto createProgrammingLanguage(CreateProgrammingLanguageRequest request);
}