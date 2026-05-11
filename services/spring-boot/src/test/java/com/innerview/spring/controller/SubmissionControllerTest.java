package com.innerview.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerview.spring.core.util.JwtUtil;
import com.innerview.spring.dto.RunCodeRequest;
import com.innerview.spring.dto.SubmissionAcceptedResponse;
import com.innerview.spring.dto.SubmissionResultDTO;
import com.innerview.spring.dto.SubmitCodeRequest;
import com.innerview.spring.enums.SubmissionStatus;
import com.innerview.spring.exception.InterviewNotActiveException;
import com.innerview.spring.exception.SubmissionExceptionHandler;
import com.innerview.spring.exception.UnsupportedSubmissionLanguageException;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.RefreshTokenService;
import com.innerview.spring.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SubmissionExceptionHandler.class)
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @Test
    void submitCode_returns202Accepted() throws Exception {
        UUID submissionId = UUID.randomUUID();
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setCode("int main(){}");
        request.setLanguage("cpp");
        request.setProblemId(UUID.randomUUID());

        when(submissionService.submitCode(eq(15L), any(), any(SubmitCodeRequest.class)))
                .thenReturn(new SubmissionAcceptedResponse(submissionId));

        mockMvc.perform(post("/api/sessions/15/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.submissionId").value(submissionId.toString()));
    }

    @Test
    void submitCode_returns409ForInactiveSession() throws Exception {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setCode("int main(){}");
        request.setLanguage("cpp");
        request.setProblemId(UUID.randomUUID());

        when(submissionService.submitCode(eq(15L), any(), any(SubmitCodeRequest.class)))
                .thenThrow(new InterviewNotActiveException("Interview session is not active."));

        mockMvc.perform(post("/api/sessions/15/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Interview session is not active."));
    }

    @Test
    void submitCode_returns400WithSupportedLanguages() throws Exception {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setCode("print(1)");
        request.setLanguage("rust");
        request.setProblemId(UUID.randomUUID());

        when(submissionService.submitCode(eq(15L), any(), any(SubmitCodeRequest.class)))
                .thenThrow(new UnsupportedSubmissionLanguageException("Unsupported language: rust", List.of("cpp", "java")));

        mockMvc.perform(post("/api/sessions/15/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported language: rust"))
                .andExpect(jsonPath("$.supportedLanguages[0]").value("cpp"))
                .andExpect(jsonPath("$.supportedLanguages[1]").value("java"));
    }

    @Test
    void getSubmissionResult_returns200DuringJudging() throws Exception {
        UUID submissionId = UUID.randomUUID();
        SubmissionResultDTO response = new SubmissionResultDTO(
                submissionId,
                10L,
                UUID.randomUUID(),
                SubmissionStatus.RUNNING,
                null,
                null,
                List.of()
        );

        when(submissionService.getSubmissionResult(eq(submissionId), any())).thenReturn(response);

        mockMvc.perform(get("/api/submissions/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void runCode_returnsExecutionSummary() throws Exception {
        UUID problemId = UUID.randomUUID();
        RunCodeRequest request = new RunCodeRequest();
        request.setCode("int main(){}");
        request.setLanguage("cpp");

        SubmissionResultDTO response = new SubmissionResultDTO(
                null,
                null,
                problemId,
                SubmissionStatus.ACCEPTED,
                100,
                19L,
                List.of()
        );

        when(submissionService.runProblem(eq(problemId), any(), any(RunCodeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/problems/{id}/run", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemId").value(problemId.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.score").value(100));
    }
}
