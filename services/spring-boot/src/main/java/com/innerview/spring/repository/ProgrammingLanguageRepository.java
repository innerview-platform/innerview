package com.innerview.spring.repository;

import com.innerview.spring.entity.ProgrammingLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgrammingLanguageRepository extends JpaRepository<ProgrammingLanguage, UUID> {
    boolean existsByNameIgnoreCase(String name);
    Optional<ProgrammingLanguage> findByNameIgnoreCase(String name);
}