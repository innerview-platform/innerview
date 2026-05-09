package com.innerview.spring.repository;

import com.innerview.spring.entity.Problem;
import com.innerview.spring.enums.Difficulty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {
    Optional<Problem> findBySlug(String slug);
    List<Problem> findAllByDifficultyAndIsActiveTrue(Difficulty difficulty);

    @Query("SELECT p FROM Problem p WHERE :tag MEMBER OF p.tags")
    List<Problem> findAllByTagsContaining(@Param("tag") String tag);

    @Query("""
        SELECT DISTINCT p FROM Problem p
        LEFT JOIN p.tags t
        WHERE p.isActive = true
          AND (:titleQuery IS NULL
               OR LOWER(p.title) LIKE LOWER(CONCAT('%', :titleQuery, '%')))
          AND (:tag IS NULL
               OR t = :tag)
          AND (:difficulty IS NULL
               OR p.difficulty = :difficulty)
        """)
    Page<Problem> search(
            @Param("titleQuery") String titleQuery,
            @Param("tag") String tag,
            @Param("difficulty") Difficulty difficulty,
            Pageable pageable
    );


}
