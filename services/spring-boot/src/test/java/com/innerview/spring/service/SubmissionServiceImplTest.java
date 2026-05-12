package com.innerview.spring.service;

import com.innerview.spring.dto.ExecutionOutcome;
import com.innerview.spring.dto.ExecutionResult;
import com.innerview.spring.dto.RunCodeRequest;
import com.innerview.spring.dto.SubmissionAcceptedResponse;
import com.innerview.spring.dto.SubmissionResultDTO;
import com.innerview.spring.dto.SubmissionTestResult;
import com.innerview.spring.dto.SubmitCodeRequest;
import com.innerview.spring.entity.Interview;
import com.innerview.spring.entity.Problem;
import com.innerview.spring.entity.ProgrammingLanguage;
import com.innerview.spring.entity.Submission;
import com.innerview.spring.entity.TestCase;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.SubmissionStatus;
import com.innerview.spring.exception.InterviewNotActiveException;
import com.innerview.spring.exception.ProblemNotInSessionException;
import com.innerview.spring.exception.UnsupportedSubmissionLanguageException;
import com.innerview.spring.repository.InterviewRepository;
import com.innerview.spring.repository.ProblemRepository;
import com.innerview.spring.repository.ProgrammingLanguageRepository;
import com.innerview.spring.repository.SubmissionRepository;
import com.innerview.spring.repository.TestCaseRepository;
import com.innerview.spring.repository.UserInterviewRepository;
import com.innerview.spring.service.impl.SubmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ProgrammingLanguageRepository programmingLanguageRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserInterviewRepository userInterviewRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private SubmissionJudgingAsyncService submissionJudgingAsyncService;

    @Mock
    private CompileServicePort compileServicePort;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private UUID currentUserId;
    private Long sessionId;
    private UUID problemId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        sessionId = 77L;
        problemId = UUID.randomUUID();

        lenient().when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            if (submission.getId() == null) {
                submission.setId(UUID.randomUUID());
            }
            return submission;
        });
    }

    @Test
    void submitCode_persistsPendingSubmission_andDispatchesAsyncJudging() {
        Interview interview = activeInterview();
        Problem problem = activeProblem(problemId);
        SubmitCodeRequest request = submitRequest(problemId, "cpp", "int main(){}");

        when(interviewRepository.findByIdWithProblems(sessionId)).thenReturn(Optional.of(interview));
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(programmingLanguageRepository.existsByNameIgnoreCase("cpp")).thenReturn(true);

        SubmissionAcceptedResponse response = submissionService.submitCode(sessionId, currentUserId, request);

        assertEquals(SubmissionStatus.PENDING, capturedSubmission().getStatus());
        assertEquals(problemId, capturedSubmission().getProblemId());
        assertEquals(currentUserId, capturedSubmission().getUserId());
        assertEquals(sessionId, capturedSubmission().getInterviewId());
        assertEquals(response.getSubmissionId(), capturedSubmission().getId());
        verify(submissionJudgingAsyncService).judgeSubmission(any(Submission.class));
    }

    @Test
    void submitCode_inactiveInterview_throwsConflictException() {
        Interview interview = activeInterview();
        interview.setStatus(InterviewStatus.COMPLETED);

        when(interviewRepository.findByIdWithProblems(sessionId)).thenReturn(Optional.of(interview));

        assertThrows(InterviewNotActiveException.class,
                () -> submissionService.submitCode(sessionId, currentUserId, submitRequest(problemId, "cpp", "code")));

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void submitCode_problemNotInSessionProblemSet_throwsValidationException() {
        Interview interview = activeInterview();
        Problem allowed = activeProblem(UUID.randomUUID());
        interview.setProblems(new ArrayList<>(List.of(allowed)));
        Problem requested = activeProblem(problemId);

        when(interviewRepository.findByIdWithProblems(sessionId)).thenReturn(Optional.of(interview));
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(requested));

        assertThrows(ProblemNotInSessionException.class,
                () -> submissionService.submitCode(sessionId, currentUserId, submitRequest(problemId, "cpp", "code")));
    }

    @Test
    void submitCode_unsupportedLanguage_throwsExceptionWithSupportedLanguageList() {
        Interview interview = activeInterview();
        Problem problem = activeProblem(problemId);
        ProgrammingLanguage cpp = language("cpp");
        ProgrammingLanguage java = language("java");

        when(interviewRepository.findByIdWithProblems(sessionId)).thenReturn(Optional.of(interview));
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(programmingLanguageRepository.existsByNameIgnoreCase("rust")).thenReturn(false);
        when(programmingLanguageRepository.findAll()).thenReturn(List.of(java, cpp));

        UnsupportedSubmissionLanguageException exception = assertThrows(
                UnsupportedSubmissionLanguageException.class,
                () -> submissionService.submitCode(sessionId, currentUserId, submitRequest(problemId, "rust", "code"))
        );

        assertEquals(List.of("cpp", "java"), exception.getSupportedLanguages());
    }

    @Test
    void getSubmissionResult_pendingSubmission_returnsPartialResponse() {
        UUID submissionId = UUID.randomUUID();
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setUserId(currentUserId);
        submission.setInterviewId(sessionId);
        submission.setProblemId(problemId);
        submission.setStatus(SubmissionStatus.RUNNING);

        when(submissionRepository.findByIdAndUserId(submissionId, currentUserId)).thenReturn(Optional.of(submission));

        SubmissionResultDTO result = submissionService.getSubmissionResult(submissionId, currentUserId);

        assertEquals(SubmissionStatus.RUNNING, result.getStatus());
        assertNull(result.getScore());
        assertNull(result.getTotalDurationMs());
        assertEquals(List.of(), result.getTestResults());
    }

    @Test
    void getSubmissionResult_finalSubmission_returnsSanitizedBreakdown() {
        UUID submissionId = UUID.randomUUID();
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setUserId(currentUserId);
        submission.setInterviewId(sessionId);
        submission.setProblemId(problemId);
        submission.setStatus(SubmissionStatus.WRONG_ANSWER);
        submission.setScore(50);
        submission.setTotalDurationMs(31L);
        submission.setTestResults(List.of(
                new SubmissionTestResult(UUID.randomUUID(), SubmissionStatus.WRONG_ANSWER, "private", "private", 11L, 100L, 2, 1, false),
                new SubmissionTestResult(UUID.randomUUID(), SubmissionStatus.ACCEPTED, "private", null, 20L, 100L, 1, 1, false)
        ));

        when(submissionRepository.findByIdAndUserId(submissionId, currentUserId)).thenReturn(Optional.of(submission));

        SubmissionResultDTO result = submissionService.getSubmissionResult(submissionId, currentUserId);

        assertEquals(SubmissionStatus.WRONG_ANSWER, result.getStatus());
        assertEquals(50, result.getScore());
        assertEquals(31L, result.getTotalDurationMs());
        assertEquals(List.of(1, 2), result.getTestResults().stream().map(test -> test.getTestIndex()).toList());
    }

    @Test
    void runProblem_executesOnlySampleTests_andSkipsRemainingAfterCompileError() {
        Problem problem = activeProblem(problemId);
        problem.setTimeLimitMs(1200);
        problem.setMemoryLimitMb(256);
        TestCase first = sampleTestCase(problem, 1, 1, "sample1", "ok");
        TestCase second = sampleTestCase(problem, 2, 2, "sample2", "ok");
        RunCodeRequest request = runCodeRequest("cpp", "bad code");

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(programmingLanguageRepository.existsByNameIgnoreCase("cpp")).thenReturn(true);
        when(testCaseRepository.findAllByProblemIdAndIsSampleTrueOrderByOrderIndexAsc(problemId))
                .thenReturn(List.of(first, second));
        when(compileServicePort.execute(eq("bad code"), eq("cpp"), eq("sample1"), eq(1200), eq(256)))
                .thenReturn(ExecutionResult.builder()
                        .outcome(ExecutionOutcome.COMPILE_ERROR)
                        .errorOutput("compile failed")
                        .durationMs(13L)
                        .build());

        SubmissionResultDTO result = submissionService.runProblem(problemId, currentUserId, request);

        assertEquals(SubmissionStatus.COMPILE_ERROR, result.getStatus());
        assertEquals(0, result.getScore());
        assertEquals(List.of(SubmissionStatus.COMPILE_ERROR, SubmissionStatus.SKIPPED),
                result.getTestResults().stream().map(test -> test.getStatus()).toList());
        verify(compileServicePort).execute("bad code", "cpp", "sample1", 1200, 256);
        verify(compileServicePort, never()).execute("bad code", "cpp", "sample2", 1200, 256);
    }

    private Submission capturedSubmission() {
        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());
        return captor.getValue();
    }

    private Interview activeInterview() {
        Interview interview = new Interview();
        interview.setId(sessionId);
        interview.setOwnerId(currentUserId);
        interview.setStatus(InterviewStatus.STARTED);
        interview.setProblems(new ArrayList<>());
        return interview;
    }

    private Problem activeProblem(UUID id) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setActive(true);
        return problem;
    }

    private ProgrammingLanguage language(String name) {
        ProgrammingLanguage programmingLanguage = new ProgrammingLanguage();
        programmingLanguage.setName(name);
        return programmingLanguage;
    }

    private SubmitCodeRequest submitRequest(UUID id, String language, String code) {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setProblemId(id);
        request.setLanguage(language);
        request.setCode(code);
        return request;
    }

    private RunCodeRequest runCodeRequest(String language, String code) {
        RunCodeRequest request = new RunCodeRequest();
        request.setLanguage(language);
        request.setCode(code);
        return request;
    }

    private TestCase sampleTestCase(Problem problem, int orderIndex, int weight, String input, String expectedOutput) {
        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID());
        testCase.setProblem(problem);
        testCase.setSample(true);
        testCase.setOrderIndex(orderIndex);
        testCase.setWeight(weight);
        testCase.setInput(input);
        testCase.setExpectedOutput(expectedOutput);
        return testCase;
    }
}
