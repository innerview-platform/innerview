package com.innerview.spring.repository;

import com.innerview.spring.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findAllByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<Submission> findAllByInterviewIdOrderBySubmittedAtDesc(Long interviewId);

    List<Submission> findAllByProblemIdOrderBySubmittedAtDesc(UUID problemId);

    List<Submission> findAllByInterviewIdAndProblemIdOrderBySubmittedAtDesc(Long interviewId, UUID problemId);

    List<Submission> findAllByUserIdAndInterviewIdOrderBySubmittedAtDesc(UUID userId, Long interviewId);

    Optional<Submission> findTopByUserIdAndInterviewIdAndProblemIdOrderBySubmittedAtDesc(
            UUID userId,
            Long interviewId,
            UUID problemId
    );
}
