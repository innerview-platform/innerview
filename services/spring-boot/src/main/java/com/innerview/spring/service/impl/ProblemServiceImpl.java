package com.innerview.spring.service.impl;

import com.innerview.spring.dto.*;
import com.innerview.spring.entity.Problem;
import com.innerview.spring.entity.TestCase;
import com.innerview.spring.entity.User;
import com.innerview.spring.enums.Difficulty;
import com.innerview.spring.exception.*;
import com.innerview.spring.exception.ProblemNotFoundException;
import com.innerview.spring.exception.ProblemRestorationException;
import com.innerview.spring.exception.UserNotFound;
import com.innerview.spring.mapper.ProblemMapper;
import com.innerview.spring.mapper.TestCaseMapper;
import com.innerview.spring.repository.ProblemRepository;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemMapper problemMapper;
    private final UserRepository userRepository;
    private final TestCaseMapper testCaseMapper;

    @Override
    public Page<ProblemResponseDTO> getAllProblems(
            String difficultyStr,
            String tag,
            String search,
            UUID createdBy,
            Boolean isActive,
            Pageable pageable) {

        // 1. Safely parse the Enum if it was provided
        Difficulty difficulty = null;
        if (difficultyStr != null && !difficultyStr.isBlank()) {
            try {
                difficulty = Difficulty.valueOf(difficultyStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // If the user sends an invalid difficulty, you can throw a custom exception
                // or just leave it null to ignore the filter.
                throw new IllegalArgumentException("Invalid difficulty level provided.");
            }
        }

        // 2. Fetch the paginated entity results from the DB
        Page<Problem> problemsPage = problemRepository.searchAllProblems(
                search, tag, difficulty, createdBy, isActive, pageable
        );

        // 3. Map the Page<Problem> directly to Page<ProblemResponseDTO>
        // Spring Data's .map() automatically applies your MapStruct method to every item
        return problemsPage.map(problemMapper::toResponseDTO);
    }

    @Override
    public Page<ProblemOwnerDTO> getAllOwnerProblems(
            String difficultyStr,
            String tag,
            String search,
            UUID createdBy,
            Boolean isActive,
            Pageable pageable) {

        // 1. Safely parse the Enum if it was provided
        Difficulty difficulty = null;
        if (difficultyStr != null && !difficultyStr.isBlank()) {
            try {
                difficulty = Difficulty.valueOf(difficultyStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // If the user sends an invalid difficulty, you can throw a custom exception
                // or just leave it null to ignore the filter.
                throw new IllegalArgumentException("Invalid difficulty level provided.");
            }
        }

        // 2. Fetch the paginated entity results from the DB
        Page<Problem> problemsPage = problemRepository.searchAllProblems(
                search, tag, difficulty, createdBy, isActive, pageable
        );

        // 3. Map the Page<Problem> directly to Page<ProblemResponseDTO>
        // Spring Data's .map() automatically applies your MapStruct method to every item
        return problemsPage.map(problemMapper::toOwnerDTO);
    }

    @Override
    public ProblemDTO getProblemBySlug(String slug, UUID currentUserId) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with slug: " + slug));
        if (currentUserId == null || !currentUserId.equals(problem.getCreatedBy())) {
            return problemMapper.toResponseDTO(problem);
        }

        return problemMapper.toOwnerDTO(problem);
    }

    @Override
    @Transactional // Ensures the creation happens in a single DB transaction
    public ProblemOwnerDTO createProblem(CreateProblemRequest request, UUID currentUserId) {

        // 1. Map request DTO to Entity
        Problem problem = problemMapper.toEntity(request);

        // 2. Generate a guaranteed unique slug using the repository
        if(problem.getSlug() == null || problem.getSlug().isBlank()) {
            String uniqueSlug = generateUniqueSlug(problem.getTitle());
            problem.setSlug(uniqueSlug);
        }

        // 3. Attach the creator using a proxy reference (Zero DB Reads!)
        User creatorProxy = userRepository.getReferenceById(currentUserId);
        problem.setCreatedBy(creatorProxy);

        // 4. Save to the database
        Problem savedProblem = problemRepository.save(problem);

        // 5. Map the saved entity back to the Owner DTO for the response
        return problemMapper.toOwnerDTO(savedProblem);
    }


    private String generateUniqueSlug(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }

        // Format the base slug to kebab-case
        String baseSlug = title.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        String currentSlug = baseSlug;
        int suffix = 1;

        while (problemRepository.existsBySlug(currentSlug)) {
            currentSlug = baseSlug + "-" + suffix;
            suffix++;
        }

        return currentSlug;
    }
    public ProblemOwnerDTO updateProblem(UUID id, UpdateProblemRequest request, UUID currentUserId) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + id));

        if (!problem.getCreatedBy().getId().equals(currentUserId)) {
            throw new AuthorizationDeniedException("You do not have permission to update this problem.");
        }


        if (request.getTitle() != null && !request.getTitle().trim().equals(problem.getTitle())) {
            problem.setSlug(generateUniqueSlug(request.getTitle()));
        }


        problemMapper.updateEntityFromRequest(request, problem);

        Problem updatedProblem = problemRepository.save(problem);
        return problemMapper.toOwnerDTO(updatedProblem);
    }

    public void deleteProblem(UUID currentUserId, UUID id) {
        Problem problem =  problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException("Problem with id: " + id));
        if (!problem.getCreatedBy().getId().equals(currentUserId)) {
            throw new AuthorizationDeniedException("You do not have permission to delete this problem.");
        }
        if (problem.isActive()) {
            problem.setActive(false);
            problemRepository.save(problem);
        }
    }

    public ProblemOwnerDTO restoreProblem(UUID id, UUID currentUserId) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException("Problem with id: " + id));
        if (!problem.getCreatedBy().getId().equals(currentUserId)) {
            throw new AuthorizationDeniedException("You do not have permission to restore this problem.");
        }
        if (!problem.isActive()) {
            problem.setActive(true);
            Problem restoredProblem = problemRepository.save(problem);
            return problemMapper.toOwnerDTO(restoredProblem);
        } else {
            throw new ProblemRestorationException("Problem is already active and cannot be restored.");
        }
    }

    @Override
    public List<TestCaseDto> getAllTestCases(UUID id, UUID currentUserId) {

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException("Problem with id: " + id));

        boolean isOwner = currentUserId != null &&
                problem.getCreatedBy().getId().equals(currentUserId);

        return problem.getTestCases().stream()
                .filter(testCase -> isOwner || testCase.isSample())
                .sorted(Comparator.comparing(TestCase::getOrderIndex))
                .map(testCaseMapper::toTestCaseDto)
                .toList();
    }

    @Transactional
    @Override
    public List<TestCaseDto> createProblemTestCase( TestCaseDto request,UUID problemId, UUID currentUserId) {

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("Problem with id: " + problemId));

        if (!problem.getCreatedBy().getId().equals(currentUserId)) {
            throw new AuthorizationDeniedException("You do not have permission to create test cases for this problem.");
        }

        if (problem.getTestCases().size() >= 100) {
            throw new MaxTestCaseException("A problem cannot have more than 100 test cases.");
        }

        TestCase testCase = testCaseMapper.toEntity(request);
        testCase.setProblem(problem);

        int nextOrderIndex = problem.getTestCases().isEmpty() ? 0 :
                problem.getTestCases().stream().mapToInt(TestCase::getOrderIndex).max().orElse(0) + 1;
        testCase.setOrderIndex(nextOrderIndex);


        problem.getTestCases().add(testCase);
        problemRepository.save(problem);

        return getAllTestCases(problemId, currentUserId);
    }

    @Override
    @Transactional
    public List<TestCaseDto> updateProblemTestCase( TestCaseDto request,UUID problemId, UUID testCaseId, UUID currentUserId) {

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("Problem with id: " + problemId));

        if (!problem.getCreatedBy().getId().equals(currentUserId)) {
            throw new AuthorizationDeniedException("You do not have permission to update this problem.");
        }

        TestCase existingTestCase = problem.getTestCases().stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new TestCaseNotFoundException("Test case not found with id: " + testCaseId));

        testCaseMapper.updateEntityFromDto(request, existingTestCase);

        problemRepository.save(problem);

        return getAllTestCases(problemId, currentUserId);
    }
    @Override
    public void deleteProblemTestCase(UUID problemId, UUID testCaseId, UUID currentUserId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("Problem with id: " + problemId));
        if (!problem.getCreatedBy().getId().equals(currentUserId)) {
            throw new AuthorizationDeniedException("You do not have permission to update this problem.");
        }
        boolean wasRemoved = problem.getTestCases().removeIf(tc -> tc.getId().equals(testCaseId));
        if (!wasRemoved) {
            throw new TestCaseNotFoundException("Test case not found with id: " + testCaseId);
        }
        problemRepository.save(problem);
    }


}