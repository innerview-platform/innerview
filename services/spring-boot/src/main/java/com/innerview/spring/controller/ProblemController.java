package com.innerview.spring.controller;

import com.innerview.spring.dto.*;
import com.innerview.spring.entity.TestCase;
import com.innerview.spring.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public ResponseEntity<Page<ProblemResponseDTO>> getActiveProblems(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        // Delegate the filtering and pagination to the service layer
        Page<ProblemResponseDTO> problems = problemService.getAllProblems(
                difficulty, tag, search, createdBy, isActive, pageable
        );

        return ResponseEntity.ok(problems);
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<ProblemOwnerDTO>> getMineProblems(
            @AuthenticationPrincipal UUID currentUserId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        Page<ProblemOwnerDTO> problems = problemService.getAllOwnerProblems(
                difficulty, tag, search, currentUserId, isActive, pageable
        );

        return ResponseEntity.ok(problems);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProblemDTO> getProblemBySlug(
            @AuthenticationPrincipal UUID currentUserId,
            @PathVariable String slug
    ) {
        ProblemDTO problem = problemService.getProblemBySlug(slug, currentUserId);
        return ResponseEntity.ok(problem);
    }

    @PostMapping
    public ResponseEntity<ProblemOwnerDTO> createProblem(
            @Valid @RequestBody CreateProblemRequest request,
            @AuthenticationPrincipal UUID currentUserId) {

        ProblemOwnerDTO problem = problemService.createProblem(request, currentUserId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{slug}")
                .buildAndExpand(problem.getSlug())
                .toUri();

        return ResponseEntity.created(location).body(problem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemOwnerDTO> updateProblem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProblemRequest request,
            @AuthenticationPrincipal UUID currentUserId) {
        ProblemOwnerDTO problemOwnerDTO = problemService.updateProblem(id,request,currentUserId);
        return ResponseEntity.ok(problemOwnerDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID currentUserId) {

        problemService.deleteProblem(id, currentUserId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<ProblemOwnerDTO> restoreProblem(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID currentUserId) {

        ProblemOwnerDTO restoredProblem = problemService.restoreProblem(id, currentUserId);

        return ResponseEntity.ok(restoredProblem);
    }

    @GetMapping("/{id}/test-cases")
    public ResponseEntity<List<TestCaseDto>> getAllTestCases(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID currentUserId) {
        List<TestCaseDto> testCases = problemService.getAllTestCases(id, currentUserId);
        return ResponseEntity.ok(testCases);
    }

    @PostMapping("/{id}/test-cases") // FIXED: Changed to POST
    public ResponseEntity<List<TestCaseDto>> createProblemTestCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID currentUserId,
            @Valid @RequestBody TestCaseDto request) { // FIXED: Added @Valid

        List<TestCaseDto> updatedTestCases = problemService.createProblemTestCase(request,id, currentUserId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();

        return ResponseEntity.created(location).body(updatedTestCases);
    }

    @PutMapping("/{id}/test-cases/{testCaseId}")
    public ResponseEntity<List<TestCaseDto>> updateProblemTestCase(
            @PathVariable UUID id,
            @PathVariable UUID testCaseId,
            @AuthenticationPrincipal UUID currentUserId,
            @Valid @RequestBody TestCaseDto request) {

        List<TestCaseDto> updatedTestCases = problemService.updateProblemTestCase( request, id,testCaseId, currentUserId);

        return ResponseEntity.ok(updatedTestCases);
    }
    @DeleteMapping("/{id}/test-cases/{testCaseId}")
    public ResponseEntity<Void> deleteProblemTestCase(
            @PathVariable UUID id,
            @PathVariable UUID testCaseId,
            @AuthenticationPrincipal UUID currentUserId) {

        problemService.deleteProblemTestCase(id, testCaseId, currentUserId);

        return ResponseEntity.noContent().build();
    }
}

