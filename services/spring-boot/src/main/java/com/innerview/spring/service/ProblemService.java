package com.innerview.spring.service;

import com.innerview.spring.dto.*;
import com.innerview.spring.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProblemService {

    Page<ProblemResponseDTO> getAllProblems(
            String difficulty,
            String tag,
            String search,
            UUID createdBy,
            Boolean isActive,
            Pageable pageable
    );

    Page<ProblemOwnerDTO> getAllOwnerProblems(
            String difficulty,
            String tag,
            String search,
            UUID createdBy,
            Boolean isActive,
            Pageable pageable
    );

    ProblemDTO getProblemBySlug(String slug, UUID currentUserId);
    ProblemOwnerDTO createProblem(CreateProblemRequest createProblemRequest, UUID currentUserId);
    ProblemOwnerDTO updateProblem(UUID id, UpdateProblemRequest request, UUID currentUserId);
    void deleteProblem(UUID id, UUID currentUserId);
    ProblemOwnerDTO restoreProblem(UUID id, UUID currentUserId);
    List<TestCaseDto> getAllTestCases(UUID id, UUID currentUserId);
    List<TestCaseDto> createProblemTestCase(TestCaseDto request, UUID problemId, UUID currentUserId);
    List<TestCaseDto> updateProblemTestCase(TestCaseDto request, UUID problemId,UUID testCaseId, UUID currentUserId);
    void deleteProblemTestCase(UUID id,UUID testCaseId, UUID currentUserId);
}