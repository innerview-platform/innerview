package com.innerview.spring.service.impl;

import com.innerview.spring.dto.*;
import com.innerview.spring.entity.*;
import com.innerview.spring.exception.*;
import com.innerview.spring.mapper.ProgrammingLanguageMapper;
import com.innerview.spring.repository.*;
import com.innerview.spring.service.UserLanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLanguageServiceImpl implements UserLanguageService {

    private final UserLanguageRepository userLanguageRepository;
    private final ProgrammingLanguageRepository programmingLanguageRepository;
    private final UserRepository userRepository;
    private final ProgrammingLanguageMapper programmingLanguageMapper;

    @Override
    @Transactional
    public MessageResponse addLanguage(UUID userId, AddLanguageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found"));

        ProgrammingLanguage language = programmingLanguageRepository
                .findById(request.getLanguageId())
                .orElseThrow(() -> new LanguageNotFoundException(
                        "Programming language not found with id: " + request.getLanguageId()));

        UserLanguageId compositeId = new UserLanguageId(userId, request.getLanguageId());

        if (userLanguageRepository.existsById(compositeId)) {
            throw new LanguageAlreadyAssignedException("Language already added to your profile");
        }

        UserLanguage userLanguage = programmingLanguageMapper.toEntity(user, language);
        userLanguage.setId(compositeId);
        userLanguageRepository.save(userLanguage);

        return new MessageResponse("Programming language added successfully");
    }

    @Override
    @Transactional
    public MessageResponse removeLanguage(UUID userId, UUID languageId) {
        UserLanguageId compositeId = new UserLanguageId(userId, languageId);

        UserLanguage userLanguage = userLanguageRepository.findById(compositeId)
                .orElseThrow(() -> new LanguageNotAssignedToUserException(
                        "Language is not assigned to your profile"));

        userLanguageRepository.delete(userLanguage);

        return new MessageResponse("Programming language removed successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgrammingLanguageDto> getUserLanguages(UUID userId) {
        List<UserLanguage> userLanguages = userLanguageRepository.findAllByIdUserId(userId);
        return programmingLanguageMapper.toDtoList(userLanguages);
    }

    @Override
    @Transactional
    public ProgrammingLanguageDto createProgrammingLanguage(CreateProgrammingLanguageRequest request) {
        if (programmingLanguageRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateLanguageNameException(
                    "Programming language '" + request.getName() + "' already exists");
        }

        ProgrammingLanguage saved = programmingLanguageRepository.save(
                programmingLanguageMapper.toEntity(request));

        return programmingLanguageMapper.toDto(saved);
    }

    @Override
    @Transactional
    public List<ProgrammingLanguageDto> getAllLanguages(){
        List<ProgrammingLanguage>languages=programmingLanguageRepository.findAll();
        return languages.stream()
                .map(programmingLanguageMapper::toDto)
                .toList();
    }
}