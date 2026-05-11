package com.innerview.spring.service;

import com.innerview.spring.dto.ExecutionOutcome;
import com.innerview.spring.dto.ExecutionResult;
import com.innerview.spring.dto.SubmissionJudgedEvent;
import com.innerview.spring.dto.SubmissionTestResult;
import com.innerview.spring.entity.Problem;
import com.innerview.spring.entity.Submission;
import com.innerview.spring.entity.TestCase;
import com.innerview.spring.enums.SubmissionStatus;
import com.innerview.spring.repository.SubmissionRepository;
import com.innerview.spring.repository.TestCaseRepository;
import com.innerview.spring.service.impl.JudgeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgeServiceImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CompileServicePort compileServicePort;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private JudgeServiceImpl judgeService;

    @BeforeEach
    void setUp() {
        lenient().when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void judge_allRequiredTestsPass_marksAcceptedWithScore100_andPublishesEvent() {
        Submission submission = submission();
        Problem problem = problem(1000, 256);
        TestCase first = testCase(problem, 2, 2, false, "1 2", "3");
        TestCase second = testCase(problem, 1, 1, false, "4 5", "9");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first, second));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(256)))
                .thenReturn(success("3", 11L, 1200L));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(second.getInput()), eq(1000), eq(256)))
                .thenReturn(success("9", 17L, 1400L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.ACCEPTED, judged.getStatus());
        assertEquals(100, judged.getScore());
        assertEquals(28L, judged.getTotalDurationMs());
        assertNull(judged.getCompilationError());
        assertEquals(2, judged.getTestResults().size());
        assertEquals(SubmissionStatus.ACCEPTED, judged.getTestResults().getFirst().getStatus());
        assertEquals(SubmissionStatus.ACCEPTED, judged.getTestResults().get(1).getStatus());

        ArgumentCaptor<SubmissionJudgedEvent> eventCaptor = ArgumentCaptor.forClass(SubmissionJudgedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        SubmissionJudgedEvent event = eventCaptor.getValue();
        assertEquals(submission.getId(), event.submissionId());
        assertEquals(submission.getInterviewId(), event.sessionId());
        assertEquals(100, event.score());
        assertEquals(SubmissionStatus.ACCEPTED, event.status());

        verify(submissionRepository, times(2)).save(submission);
        var inOrder = inOrder(compileServicePort);
        inOrder.verify(compileServicePort).execute(submission.getCode(), submission.getLanguage(), second.getInput(), 1000, 256);
        inOrder.verify(compileServicePort).execute(submission.getCode(), submission.getLanguage(), first.getInput(), 1000, 256);
    }

    @Test
    void judge_partialPasses_calculatesWeightedRoundedScore() {
        Submission submission = submission();
        Problem problem = problem(1500, 128);
        TestCase first = testCase(problem, 3, 1, false, "a", "ok");
        TestCase second = testCase(problem, 1, 2, false, "b", "ok");
        TestCase third = testCase(problem, 2, 2, false, "c", "ok");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(second, third, first));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(second.getInput()), eq(1500), eq(128)))
                .thenReturn(success("ok", 10L, 1000L));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(third.getInput()), eq(1500), eq(128)))
                .thenReturn(success("wrong", 15L, 1200L));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1500), eq(128)))
                .thenReturn(success("ok", 20L, 1400L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.WRONG_ANSWER, judged.getStatus());
        assertEquals(60, judged.getScore());
        assertEquals(List.of(1, 2, 3), judged.getTestResults().stream().map(SubmissionTestResult::getOrderIndex).toList());
    }

    @Test
    void judge_allFailWrongAnswer_returnsWrongAnswerWithZeroScore() {
        Submission submission = submission();
        Problem problem = problem(1000, 64);
        TestCase first = testCase(problem, 1, 1, false, "in1", "1");
        TestCase second = testCase(problem, 2, 1, false, "in2", "2");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first, second));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), any(String.class), eq(1000), eq(64)))
                .thenReturn(success("x", 9L, 1000L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.WRONG_ANSWER, judged.getStatus());
        assertEquals(0, judged.getScore());
        assertEquals(List.of(SubmissionStatus.WRONG_ANSWER, SubmissionStatus.WRONG_ANSWER),
                judged.getTestResults().stream().map(SubmissionTestResult::getStatus).toList());
    }

    @Test
    void judge_firstExecutionCompileError_skipsRemainingTests_andForcesZeroScore() {
        Submission submission = submission();
        Problem problem = problem(1000, 128);
        TestCase first = testCase(problem, 1, 1, false, "in1", "1");
        TestCase second = testCase(problem, 2, 1, false, "in2", "2");
        TestCase third = testCase(problem, 3, 1, true, "in3", "3");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first, second, third));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(128)))
                .thenReturn(failure(ExecutionOutcome.COMPILE_ERROR, "compile failed", 13L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.COMPILE_ERROR, judged.getStatus());
        assertEquals(0, judged.getScore());
        assertEquals("compile failed", judged.getCompilationError());
        assertEquals(List.of(
                        SubmissionStatus.COMPILE_ERROR,
                        SubmissionStatus.SKIPPED,
                        SubmissionStatus.SKIPPED
                ),
                judged.getTestResults().stream().map(SubmissionTestResult::getStatus).toList());
        verify(compileServicePort, times(1))
                .execute(submission.getCode(), submission.getLanguage(), first.getInput(), 1000, 128);
    }

    @Test
    void judge_runtimeError_returnsRuntimeError() {
        Submission submission = submission();
        Problem problem = problem(1000, 64);
        TestCase first = testCase(problem, 1, 1, false, "in", "ok");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(64)))
                .thenReturn(failure(ExecutionOutcome.RUNTIME_ERROR, "segfault", 21L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.RUNTIME_ERROR, judged.getStatus());
        assertEquals("segfault", judged.getTestResults().getFirst().getErrorOutput());
    }

    @Test
    void judge_timeout_returnsTimeLimitExceeded() {
        Submission submission = submission();
        Problem problem = problem(500, 64);
        TestCase first = testCase(problem, 1, 1, false, "in", "ok");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(500), eq(64)))
                .thenReturn(failure(ExecutionOutcome.TIME_LIMIT_EXCEEDED, "timeout", 500L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.TIME_LIMIT_EXCEEDED, judged.getStatus());
        assertEquals(SubmissionStatus.TIME_LIMIT_EXCEEDED, judged.getTestResults().getFirst().getStatus());
    }

    @Test
    void judge_memoryExceeded_returnsMemoryLimitExceeded() {
        Submission submission = submission();
        Problem problem = problem(1000, 32);
        TestCase first = testCase(problem, 1, 1, false, "in", "ok");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(32)))
                .thenReturn(failure(ExecutionOutcome.MEMORY_LIMIT_EXCEEDED, "mle", 45L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.MEMORY_LIMIT_EXCEEDED, judged.getStatus());
        assertEquals(SubmissionStatus.MEMORY_LIMIT_EXCEEDED, judged.getTestResults().getFirst().getStatus());
    }

    @Test
    void judge_mixedVerdicts_prefersTimeLimitOverWrongAnswerAndRuntimeError() {
        Submission submission = submission();
        Problem problem = problem(1000, 128);
        TestCase first = testCase(problem, 1, 1, false, "a", "ok");
        TestCase second = testCase(problem, 2, 1, false, "b", "ok");
        TestCase third = testCase(problem, 3, 1, false, "c", "ok");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first, second, third));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(128)))
                .thenReturn(success("bad", 10L, 1000L));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(second.getInput()), eq(1000), eq(128)))
                .thenReturn(failure(ExecutionOutcome.RUNTIME_ERROR, "runtime", 11L));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(third.getInput()), eq(1000), eq(128)))
                .thenReturn(failure(ExecutionOutcome.TIME_LIMIT_EXCEEDED, "timeout", 12L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.TIME_LIMIT_EXCEEDED, judged.getStatus());
    }

    @Test
    void judge_mixedVerdicts_prefersWrongAnswerOverRuntimeError() {
        Submission submission = submission();
        Problem problem = problem(1000, 128);
        TestCase first = testCase(problem, 1, 1, false, "a", "ok");
        TestCase second = testCase(problem, 2, 1, false, "b", "ok");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first, second));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(128)))
                .thenReturn(failure(ExecutionOutcome.RUNTIME_ERROR, "runtime", 10L));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(second.getInput()), eq(1000), eq(128)))
                .thenReturn(success("bad", 10L, 1000L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.WRONG_ANSWER, judged.getStatus());
    }

    @Test
    void judge_trailingWhitespaceDifferences_areIgnored() {
        Submission submission = submission();
        Problem problem = problem(1000, 64);
        TestCase first = testCase(problem, 1, 1, false, "in", "Hello World\n");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(64)))
                .thenReturn(success("Hello World   \n\n", 10L, 500L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.ACCEPTED, judged.getStatus());
        assertEquals(100, judged.getScore());
    }

    @Test
    void judge_sampleFailureDoesNotBlockAccepted_whenRequiredTestsPass() {
        Submission submission = submission();
        Problem problem = problem(1000, 64);
        TestCase sample = testCase(problem, 1, 1, true, "sample", "sample-ok");
        TestCase required = testCase(problem, 2, 3, false, "real", "real-ok");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(sample, required));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(sample.getInput()), eq(1000), eq(64)))
                .thenReturn(success("wrong", 10L, 500L));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(required.getInput()), eq(1000), eq(64)))
                .thenReturn(success("real-ok", 10L, 500L));

        Submission judged = judgeService.judge(submission);

        assertEquals(SubmissionStatus.ACCEPTED, judged.getStatus());
        assertEquals(75, judged.getScore());
    }

    @Test
    void judge_persistsRunningThenFinalStatus_withoutBackwardTransitions() {
        Submission submission = submission();
        Problem problem = problem(1000, 64);
        TestCase first = testCase(problem, 1, 1, false, "in", "ok");
        List<SubmissionStatus> savedStatuses = new java.util.ArrayList<>();

        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });
        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(64)))
                .thenReturn(success("ok", 10L, 100L));

        judgeService.judge(submission);

        assertEquals(List.of(SubmissionStatus.RUNNING, SubmissionStatus.ACCEPTED), savedStatuses);
    }

    @Test
    void judge_rejectsAlreadyFinalSubmission() {
        Submission submission = submission();
        submission.setStatus(SubmissionStatus.ACCEPTED);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> judgeService.judge(submission));

        assertEquals("Submission cannot transition from ACCEPTED to RUNNING.", exception.getMessage());
    }

    @Test
    void judge_publishesConcreteSubmissionJudgedEvent() {
        Submission submission = submission();
        Problem problem = problem(1000, 64);
        TestCase first = testCase(problem, 1, 1, false, "in", "ok");

        when(testCaseRepository.findAllByProblemIdOrderByOrderIndexAsc(submission.getProblemId()))
                .thenReturn(List.of(first));
        when(compileServicePort.execute(eq(submission.getCode()), eq(submission.getLanguage()), eq(first.getInput()), eq(1000), eq(64)))
                .thenReturn(success("ok", 10L, 100L));

        judgeService.judge(submission);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        Object event = eventCaptor.getValue();
        assertNotNull(event);
        assertInstanceOf(SubmissionJudgedEvent.class, event);
    }

    private Submission submission() {
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID());
        submission.setUserId(UUID.randomUUID());
        submission.setInterviewId(42L);
        submission.setProblemId(UUID.randomUUID());
        submission.setCode("int main() { return 0; }");
        submission.setLanguage("cpp");
        submission.setStatus(SubmissionStatus.PENDING);
        return submission;
    }

    private Problem problem(int timeLimitMs, int memoryLimitMb) {
        Problem problem = new Problem();
        problem.setTimeLimitMs(timeLimitMs);
        problem.setMemoryLimitMb(memoryLimitMb);
        return problem;
    }

    private TestCase testCase(
            Problem problem,
            int orderIndex,
            int weight,
            boolean sample,
            String input,
            String expectedOutput
    ) {
        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID());
        testCase.setProblem(problem);
        testCase.setOrderIndex(orderIndex);
        testCase.setWeight(weight);
        testCase.setSample(sample);
        testCase.setInput(input);
        testCase.setExpectedOutput(expectedOutput);
        return testCase;
    }

    private ExecutionResult success(String actualOutput, long durationMs, long memoryBytes) {
        return ExecutionResult.builder()
                .outcome(ExecutionOutcome.SUCCESS)
                .actualOutput(actualOutput)
                .durationMs(durationMs)
                .memoryBytes(memoryBytes)
                .build();
    }

    private ExecutionResult failure(ExecutionOutcome outcome, String errorOutput, long durationMs) {
        return ExecutionResult.builder()
                .outcome(outcome)
                .errorOutput(errorOutput)
                .durationMs(durationMs)
                .build();
    }
}
