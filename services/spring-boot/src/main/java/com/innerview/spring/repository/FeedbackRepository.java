package com.innerview.spring.repository;

import com.innerview.spring.dto.FeedbackDto;
import com.innerview.spring.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("""
        SELECT new com.innerview.spring.dto.FeedbackDto(
            f.rating, f.comment, f.reviewer.id, f.interview.id, f.createdAt
        )
        FROM Feedback f
        WHERE f.reviewee.id = :userId
        AND (:rating IS NULL OR f.rating = :rating)
    """)
    Page<FeedbackDto> findFeedbackReceivedByUser(
            @Param("userId") UUID userId,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    @Query("""
        SELECT new com.innerview.spring.dto.FeedbackDto(
            f.rating, f.comment, f.reviewee.id, f.interview.id, f.createdAt
        )
        FROM Feedback f
        WHERE f.reviewer.id = :userId
    """)
    Page<FeedbackDto> findFeedbackGivenByUser(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}