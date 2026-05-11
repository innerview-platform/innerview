package com.innerview.spring.service.impl;

import com.innerview.spring.dto.ExecutionOutcome;
import com.innerview.spring.dto.ExecutionResult;
import com.innerview.spring.dto.RunCodeRequest;
import com.innerview.spring.dto.SubmissionAcceptedResponse;
import com.innerview.spring.dto.SubmissionResultDTO;
import com.innerview.spring.dto.SubmissionTestCaseResultDTO;
import com.innerview.spring.dto.SubmissionTestResult;
import com.innerview.spring.dto.SubmitCodeRequest;
import com.innerview.spring.entity.Interview;
import com.innerview.spring.entity.Problem;
import com.innerview.spring.entity.Submission;
import com.innerview.spring.entity.TestCase;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.SubmissionStatus;
import com.innerview.spring.exception.InterviewNotActiveException;
import com.innerview.spring.exception.InterviewNotFoundException;
import com.innerview.spring.exception.ProblemNotFoundException;
import com.innerview.spring.exception.ProblemNotInSessionException;
import com.innerview.spring.exception.SubmissionNotFoundException;
import com.innerview.spring.exception.UnsupportedSubmissionLanguageException;
import com.innerview.spring.repository.InterviewRepository;
import com.innerview.spring.repository.ProblemRepository;
import com.innerview.spring.repository.ProgrammingLanguageRepository;
import com.innerview.spring.repository.SubmissionRepository;
import com.innerview.spring.repository.TestCaseRepository;
import com.innerview.spring.repository.UserInterviewRepository;
import com.innerview.spring.service.CompileServicePort;
import com.innerview.spring.service.SubmissionJudgingAsyncService;
import com.innerview.spring.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final InterviewRepository interviewRepository;
    private final ProblemRepository problemRepository;
    private final ProgrammingLanguageRepository programmingLanguageRepository;
    private final SubmissionRepository submissionRepository;
    private final UserInterviewRepository userInterviewRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionJudgingAsyncService submissionJudgingAsyncService;
    private final CompileServicePort compileServicePort;

    @Override
    public SubmissionAcceptedResponse submitCode(Long sessionId, UUID currentUserId, SubmitCodeRequest request) {
        Interview interview = loadAccessibleInterview(sessionId, currentUserId);
        ensureInterviewActive(interview);
        Problem problem = loadActiveProblem(request.getProblemId());
        ensureProblemBelongsToSession(interview, problem.getId());
        validateLanguage(request.getLanguage());

        Submission submission = new Submission();
        submission.setUserId(currentUserId);
        submission.setInterviewId(interview.getId());
        submission.setProblemId(problem.getId());
        submission.setCode(request.getCode());
        submission.setLanguage(request.getLanguage());
        submission.setStatus(SubmissionStatus.PENDING);

        Submission savedSubmission = submissionRepository.save(submission);
        submissionJudgingAsyncService.judgeSubmission(savedSubmission);

        return new SubmissionAcceptedResponse(savedSubmission.getId());
    }

    @Override
    public SubmissionResultDTO getSubmissionResult(UUID submissionId, UUID currentUserId) {
        Submission submission = submissionRepository.findByIdAndUserId(submissionId, currentUserId)
                .orElseThrow(() -> new SubmissionNotFoundException("Submission not found: " + submissionId));

        if (submission.getStatus() == SubmissionStatus.PENDING || submission.getStatus() == SubmissionStatus.RUNNING) {
            return new SubmissionResultDTO(
                    submission.getId(),
                    submission.getInterviewId(),
                    submission.getProblemId(),
                    submission.getStatus(),
                    null,
                    null,
                    List.of()
            );
        }

        return toResultDto(submission);
    }

    @Override
    public SubmissionResultDTO runProblem(UUID problemId, UUID currentUserId, RunCodeRequest request) {
        Problem problem = loadActiveProblem(problemId);
        validateLanguage(request.getLanguage());

        List<TestCase> sampleTestCases = testCaseRepository.findAllByProblemIdAndIsSampleTrueOrderByOrderIndexAsc(problemId);
        List<SubmissionTestCaseResultDTO> results = new ArrayList<>();
        boolean compileErrorEncountered = false;
        int passedWeight = 0;
        int totalWeight = sampleTestCases.stream().mapToInt(TestCase::getWeight).sum();
        long totalDurationMs = 0L;
        List<SubmissionStatus> verdicts = new ArrayList<>();

        for (TestCase testCase : sampleTestCases) {
            if (compileErrorEncountered) {
                results.add(new SubmissionTestCaseResultDTO(testCase.getOrderIndex(), SubmissionStatus.SKIPPED, 0L));
                verdicts.add(SubmissionStatus.SKIPPED);
                continue;
            }

            ExecutionResult executionResult = compileServicePort.execute(
                    request.getCode(),
                    request.getLanguage(),
                    testCase.getInput(),
                    problem.getTimeLimitMs(),
                    problem.getMemoryLimitMb()
            );

            SubmissionStatus verdict = resolveVerdict(testCase, executionResult);
            results.add(new SubmissionTestCaseResultDTO(
                    testCase.getOrderIndex(),
                    verdict,
                    executionResult == null || executionResult.getDurationMs() == null ? 0L : executionResult.getDurationMs()
            ));
            verdicts.add(verdict);
            totalDurationMs += executionResult == null || executionResult.getDurationMs() == null ? 0L : executionResult.getDurationMs();

            if (verdict == SubmissionStatus.ACCEPTED) {
                passedWeight += testCase.getWeight();
            }
            if (verdict == SubmissionStatus.COMPILE_ERROR) {
                compileErrorEncountered = true;
            }
        }

        SubmissionStatus finalStatus = determineWorstVerdict(verdicts);
        Integer score = compileErrorEncountered || totalWeight == 0
                ? 0
                : (int) Math.round((passedWeight * 100.0) / totalWeight);

        return new SubmissionResultDTO(
                null,
                null,
                problemId,
                finalStatus,
                score,
                totalDurationMs,
                results
        );
    }

    private Interview loadAccessibleInterview(Long interviewId, UUID currentUserId) {
        Interview interview = interviewRepository.findByIdWithProblems(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException("Interview session not found: " + interviewId));

        boolean isOwner = currentUserId.equals(interview.getOwnerId());
        boolean isParticipant = userInterviewRepository.existsByIdInterviewIdAndIdUserId(interviewId, currentUserId);
        if (!isOwner && !isParticipant) {
            throw new InterviewNotFoundException("Interview session not found: " + interviewId);
        }
        return interview;
    }

    private void ensureInterviewActive(Interview interview) {
        if (interview.getStatus() != InterviewStatus.STARTED && interview.getStatus() != InterviewStatus.ACTIVE) {
            throw new InterviewNotActiveException("Interview session is not active.");
        }
    }

    private Problem loadActiveProblem(UUID problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("Problem with id: " + problemId));
        if (!problem.isActive()) {
            throw new ProblemNotFoundException("Problem with id: " + problemId);
        }
        return problem;
    }

    private void ensureProblemBelongsToSession(Interview interview, UUID problemId) {
        if (interview.getProblems() == null || interview.getProblems().isEmpty()) {
            return;
        }
        boolean found = interview.getProblems().stream().anyMatch(problem -> problem.getId().equals(problemId));
        if (!found) {
            throw new ProblemNotInSessionException("Problem does not belong to the session problem set.");
        }
    }

    private void validateLanguage(String language) {
        if (programmingLanguageRepository.existsByNameIgnoreCase(language)) {
            return;
        }

        List<String> supportedLanguages = programmingLanguageRepository.findAll().stream()
                .map(programmingLanguage -> programmingLanguage.getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        throw new UnsupportedSubmissionLanguageException(
                "Unsupported language: " + language,
                supportedLanguages
        );
    }

    private SubmissionResultDTO toResultDto(Submission submission) {
        List<SubmissionTestCaseResultDTO> testResults = submission.getTestResults().stream()
                .sorted(Comparator.comparing(result -> result.getOrderIndex() == null ? Integer.MAX_VALUE : result.getOrderIndex()))
                .map(result -> new SubmissionTestCaseResultDTO(
                        result.getOrderIndex(),
                        result.getStatus(),
                        result.getDurationMs()
                ))
                .toList();

        return new SubmissionResultDTO(
                submission.getId(),
                submission.getInterviewId(),
                submission.getProblemId(),
                submission.getStatus(),
                submission.getScore(),
                submission.getTotalDurationMs(),
                testResults
        );
    }

    private SubmissionStatus resolveVerdict(TestCase testCase, ExecutionResult executionResult) {
        if (executionResult == null || executionResult.getOutcome() == null) {
            return SubmissionStatus.RUNTIME_ERROR;
        }

        if (executionResult.getOutcome() == ExecutionOutcome.SUCCESS) {
            String actual = executionResult.getActualOutput() == null ? "" : executionResult.getActualOutput().trim();
            String expected = testCase.getExpectedOutput() == null ? "" : testCase.getExpectedOutput().trim();
            return actual.equals(expected) ? SubmissionStatus.ACCEPTED : SubmissionStatus.WRONG_ANSWER;
        }

        return switch (executionResult.getOutcome()) {
            case COMPILE_ERROR -> SubmissionStatus.COMPILE_ERROR;
            case TIME_LIMIT_EXCEEDED -> SubmissionStatus.TIME_LIMIT_EXCEEDED;
            case MEMORY_LIMIT_EXCEEDED -> SubmissionStatus.MEMORY_LIMIT_EXCEEDED;
            case RUNTIME_ERROR -> SubmissionStatus.RUNTIME_ERROR;
            case SUCCESS -> SubmissionStatus.ACCEPTED;
        };
    }

    private SubmissionStatus determineWorstVerdict(List<SubmissionStatus> verdicts) {
        if (verdicts.isEmpty()) {
            return SubmissionStatus.ACCEPTED;
        }
        if (verdicts.stream().allMatch(status -> status == SubmissionStatus.ACCEPTED || status == SubmissionStatus.SKIPPED)) {
            return SubmissionStatus.ACCEPTED;
        }

        return verdicts.stream()
                .filter(status -> status != SubmissionStatus.ACCEPTED && status != SubmissionStatus.SKIPPED)
                .max(Comparator.comparingInt(this::priorityOf))
                .orElse(SubmissionStatus.ACCEPTED);
    }

    private int priorityOf(SubmissionStatus status) {
        return switch (status) {
            case RUNTIME_ERROR -> 1;
            case WRONG_ANSWER -> 2;
            case MEMORY_LIMIT_EXCEEDED -> 3;
            case TIME_LIMIT_EXCEEDED -> 4;
            case COMPILE_ERROR -> 5;
            default -> 0;
        };
    }
}
