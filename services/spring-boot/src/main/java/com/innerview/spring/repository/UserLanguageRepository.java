package com.innerview.spring.repository;

import com.innerview.spring.entity.UserLanguage;
import com.innerview.spring.entity.UserLanguageId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserLanguageRepository extends JpaRepository<UserLanguage, UserLanguageId> {

    boolean existsById(UserLanguageId id);

    Optional<UserLanguage> findById(UserLanguageId id);

    List<UserLanguage> findAllByIdUserId(UUID userId);
}