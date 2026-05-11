package com.innerview.spring.service.impl;

import com.innerview.spring.dto.ExecutionOutcome;
import com.innerview.spring.dto.ExecutionResult;
import com.innerview.spring.dto.SubmissionJudgedEvent;
import com.innerview.spring.dto.SubmissionTestResult;
import com.innerview.spring.entity.Submission;
import com.innerview.spring.entity.TestCase;
import com.innerview.spring.enums.SubmissionStatus;
import com.innerview.spring.repository.SubmissionRepository;
import com.innerview.spring.repository.TestCaseRepository;
import com.innerview.spring.service.CompileServicePort;
import com.innerview.spring.service.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private static final Map<SubmissionStatus, Integer> VERDICT_PRIORITY = buildVerdictPriority();

    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final CompileServicePort compileServicePort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Submission judge(Submission submission) {
        ensureJudgeable(submission);

        if (submission.getStatus() == SubmissionStatus.PENDING) {
            submission.setStatus(SubmissionStatus.RUNNING);
            submissionRepository.save(submission);
        }

        List<TestCase> testCases = new ArrayList<>(
                testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId())
        );
        testCases.sort(Comparator.comparing(TestCase::getOrderIndex));

        List<SubmissionTestResult> testResults = new ArrayList<>(testCases.size());
        String compilationError = null;
        boolean compileErrorEncountered = false;
        long totalDurationMs = 0L;
        int passedWeight = 0;
        int totalWeight = testCases.stream().mapToInt(TestCase::getWeight).sum();

        for (int index = 0; index < testCases.size(); index++) {
            TestCase testCase = testCases.get(index);

            if (compileErrorEncountered) {
                testResults.add(buildSkippedResult(testCase));
                continue;
            }

            ExecutionResult executionResult = compileServicePort.execute(
                    submission.getCode(),
                    submission.getLanguage(),
                    testCase.getInput(),
                    testCase.getProblem().getTimeLimitMs(),
                    testCase.getProblem().getMemoryLimitMb()
            );

            SubmissionStatus verdict = resolveVerdict(testCase, executionResult);
            totalDurationMs += safeDuration(executionResult.getDurationMs());

            if (verdict == SubmissionStatus.ACCEPTED) {
                passedWeight += testCase.getWeight();
            }

            if (verdict == SubmissionStatus.COMPILE_ERROR) {
                compileErrorEncountered = true;
                compilationError = executionResult.getErrorOutput();
            }

            testResults.add(buildResult(testCase, verdict, executionResult));
        }

        submission.setTestResults(testResults);
        submission.setCompilationError(compilationError);
        submission.setTotalDurationMs(totalDurationMs);
        submission.setScore(calculateScore(compileErrorEncountered, passedWeight, totalWeight));

        SubmissionStatus finalStatus = determineFinalStatus(testCases, testResults, compileErrorEncountered);
        transitionToFinalStatus(submission, finalStatus);

        Submission savedSubmission = submissionRepository.save(submission);
        applicationEventPublisher.publishEvent(new SubmissionJudgedEvent(
                savedSubmission.getId(),
                savedSubmission.getInterviewId(),
                savedSubmission.getScore(),
                savedSubmission.getStatus()
        ));

        return savedSubmission;
    }

    private void ensureJudgeable(Submission submission) {
        if (submission.getStatus() != SubmissionStatus.PENDING && submission.getStatus() != SubmissionStatus.RUNNING) {
            throw new IllegalStateException("Submission cannot transition from " + submission.getStatus() + " to RUNNING.");
        }
    }

    private SubmissionStatus resolveVerdict(TestCase testCase, ExecutionResult executionResult) {
        if (executionResult == null || executionResult.getOutcome() == null) {
            return SubmissionStatus.RUNTIME_ERROR;
        }

        if (executionResult.getOutcome() == ExecutionOutcome.SUCCESS) {
            String actual = normalize(executionResult.getActualOutput());
            String expected = normalize(testCase.getExpectedOutput());
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

    private SubmissionTestResult buildResult(
            TestCase testCase,
            SubmissionStatus verdict,
            ExecutionResult executionResult
    ) {
        SubmissionTestResult testResult = new SubmissionTestResult();
        testResult.setTestCaseId(testCase.getId());
        testResult.setStatus(verdict);
        testResult.setActualOutput(executionResult == null ? null : executionResult.getActualOutput());
        testResult.setErrorOutput(executionResult == null ? null : executionResult.getErrorOutput());
        testResult.setDurationMs(executionResult == null ? 0L : safeDuration(executionResult.getDurationMs()));
        testResult.setMemoryBytes(executionResult == null ? null : executionResult.getMemoryBytes());
        testResult.setOrderIndex(testCase.getOrderIndex());
        testResult.setWeight(testCase.getWeight());
        testResult.setSample(testCase.isSample());
        return testResult;
    }

    private SubmissionTestResult buildSkippedResult(TestCase testCase) {
        SubmissionTestResult testResult = new SubmissionTestResult();
        testResult.setTestCaseId(testCase.getId());
        testResult.setStatus(SubmissionStatus.SKIPPED);
        testResult.setDurationMs(0L);
        testResult.setOrderIndex(testCase.getOrderIndex());
        testResult.setWeight(testCase.getWeight());
        testResult.setSample(testCase.isSample());
        return testResult;
    }

    private int calculateScore(boolean compileErrorEncountered, int passedWeight, int totalWeight) {
        if (compileErrorEncountered || totalWeight == 0) {
            return 0;
        }
        return (int) Math.round((passedWeight * 100.0) / totalWeight);
    }

    private SubmissionStatus determineFinalStatus(
            List<TestCase> testCases,
            List<SubmissionTestResult> testResults,
            boolean compileErrorEncountered
    ) {
        if (compileErrorEncountered) {
            return SubmissionStatus.COMPILE_ERROR;
        }

        List<SubmissionTestResult> requiredResults = new ArrayList<>();
        for (int index = 0; index < testCases.size(); index++) {
            if (!testCases.get(index).isSample()) {
                requiredResults.add(testResults.get(index));
            }
        }

        List<SubmissionTestResult> resultsToEvaluate = requiredResults.isEmpty() ? testResults : requiredResults;

        boolean allRequiredAccepted = resultsToEvaluate.stream()
                .allMatch(result -> result.getStatus() == SubmissionStatus.ACCEPTED);
        if (allRequiredAccepted) {
            return SubmissionStatus.ACCEPTED;
        }

        return resultsToEvaluate.stream()
                .map(SubmissionTestResult::getStatus)
                .filter(status -> status != SubmissionStatus.ACCEPTED && status != SubmissionStatus.SKIPPED)
                .max(Comparator.comparingInt(status -> VERDICT_PRIORITY.getOrDefault(status, 0)))
                .orElse(SubmissionStatus.WRONG_ANSWER);
    }

    private void transitionToFinalStatus(Submission submission, SubmissionStatus finalStatus) {
        if (submission.getStatus() == SubmissionStatus.PENDING) {
            throw new IllegalStateException("Submission must be RUNNING before a final verdict is assigned.");
        }
        submission.setStatus(finalStatus);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private long safeDuration(Long value) {
        return value == null ? 0L : value;
    }

    private static Map<SubmissionStatus, Integer> buildVerdictPriority() {
        Map<SubmissionStatus, Integer> priority = new EnumMap<>(SubmissionStatus.class);
        priority.put(SubmissionStatus.RUNTIME_ERROR, 1);
        priority.put(SubmissionStatus.WRONG_ANSWER, 2);
        priority.put(SubmissionStatus.MEMORY_LIMIT_EXCEEDED, 3);
        priority.put(SubmissionStatus.TIME_LIMIT_EXCEEDED, 4);
        priority.put(SubmissionStatus.COMPILE_ERROR, 5);
        return priority;
    }
}
