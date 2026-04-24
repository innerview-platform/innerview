package com.innerview.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerview.spring.dto.*;
import com.innerview.spring.enums.ExperienceLevel;
import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.exception.UserHasProfile;
import com.innerview.spring.exception.UserExceptionHandler;
import com.innerview.spring.exception.UserProfileNotFound;
import com.innerview.spring.exception.UserNotFound;
import com.innerview.spring.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class UserProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UserProfileController userProfileController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();

        // Setup MockMvc with custom ArgumentResolver to mock @AuthenticationPrincipal
        mockMvc = MockMvcBuilders.standaloneSetup(userProfileController)
                .setControllerAdvice(new UserExceptionHandler()) // Wire in your exception handler
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(UUID.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return currentUserId;
                    }
                })
                .build();
    }


    @Test
    void createProfile_ShouldCreateProfileSuccessfully() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest();
        request.setBio("Backend Developer");

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(currentUserId);
        response.setBio("Backend Developer");

        when(userProfileService.createProfile(eq(currentUserId), any(CreateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user_id").value(currentUserId.toString()))
                .andExpect(jsonPath("$.bio").value("Backend Developer"));
    }

    @Test
    void createProfile_ShouldFailIfProfileAlreadyExists() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest();

        when(userProfileService.createProfile(any(), any()))
                .thenThrow(new UserHasProfile("User already has a profile"));

        mockMvc.perform(post("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("User already has a profile"));
    }

    @Test
    void getProfile_ShouldReturnUserProfileSuccessfully() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(currentUserId);
        response.setBio("Existing Profile");

        when(userProfileService.findProfileById(currentUserId)).thenReturn(response);

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(currentUserId.toString()))
                .andExpect(jsonPath("$.bio").value("Existing Profile"));
    }

    @Test
    void getProfile_ShouldReturn404IfProfileDoesNotExist() throws Exception {
        when(userProfileService.findProfileById(currentUserId))
                .thenThrow(new UserProfileNotFound("Profile not found"));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Profile not found"));
    }

    @Test
    void updateProfile_ShouldUpdateProfileSuccessfully() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest();
        request.setBio("Updated Bio");

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(currentUserId);
        response.setBio("Updated Bio");

        when(userProfileService.UpdateProfile(eq(currentUserId), any(CreateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated Bio"));
    }

    @Test
    void updateProfile_ShouldReturn404IfProfileNotFound() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest();

        when(userProfileService.UpdateProfile(any(), any()))
                .thenThrow(new UserProfileNotFound("Profile not found"));

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProfile_ShouldDeleteProfileSuccessfully() throws Exception {
        doNothing().when(userProfileService).deleteUserProfileById(currentUserId);

        mockMvc.perform(delete("/api/profile"))
                .andExpect(status().isOk());

        verify(userProfileService, times(1)).deleteUserProfileById(currentUserId);
    }

    @Test
    void deleteProfile_ShouldReturn404IfProfileNotFound() throws Exception {
        doThrow(new UserProfileNotFound("Profile not found")).when(userProfileService).deleteUserProfileById(currentUserId);

        mockMvc.perform(delete("/api/profile"))
                .andExpect(status().isNotFound());
    }


    @Test
    void getUserAverageRating_ShouldReturnCorrectAverageRating() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UserAverageRatingResponse response = new UserAverageRatingResponse(targetUserId, 4.5, 10L);

        when(userProfileService.getAverageRatingById(targetUserId)).thenReturn(response);

        mockMvc.perform(get("/api/profile/{userId}/rating", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(targetUserId.toString()))
                .andExpect(jsonPath("$.average_rating").value(4.5))
                .andExpect(jsonPath("$.total_reviews").value(10));
    }

    @Test
    void getUserAverageRating_ShouldReturn0IfNoRatingsExist() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UserAverageRatingResponse response = new UserAverageRatingResponse(targetUserId, 0.0, 0L);

        when(userProfileService.getAverageRatingById(targetUserId)).thenReturn(response);

        mockMvc.perform(get("/api/profile/{userId}/rating", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average_rating").value(0.0))
                .andExpect(jsonPath("$.total_reviews").value(0));
    }

    @Test
    void getUserAverageRating_ShouldReturn404IfUserNotFound() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        when(userProfileService.getAverageRatingById(targetUserId))
                .thenThrow(new UserNotFound("User not found"));

        mockMvc.perform(get("/api/profile/{userId}/rating", targetUserId))
                .andExpect(status().isNotFound());
    }



    @Test
    void getUserInterviews_ShouldReturnUserInterviews() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        InterviewHistoryDto interview = new InterviewHistoryDto(1L, "MOCK", LocalDateTime.now(), 60, "INTERVIEWER");

        // FIX: Add PageRequest.of() and total elements
        Page<InterviewHistoryDto> page = new PageImpl<>(List.of(interview), PageRequest.of(0, 10), 1);

        when(userProfileService.getUserInterviewHistory(eq(targetUserId), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/profile/{userId}/interviews", targetUserId)
                        .param("page", "0")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].interview_id").value(1))
                .andExpect(jsonPath("$.content[0].type").value("MOCK"));
    }

    @Test
    void getUserInterviews_ShouldReturnEmptyListIfNoInterviewsExist() throws Exception {
        UUID targetUserId = UUID.randomUUID();

        // FIX: Add PageRequest.of() and total elements
        Page<InterviewHistoryDto> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(userProfileService.getUserInterviewHistory(eq(targetUserId), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/profile/{userId}/interviews", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ==================== 4. Feedback Endpoints ====================

    @Test
    void getReceivedFeedback_ShouldReturnFeedbackList() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        FeedbackDto feedback = new FeedbackDto(5, "Great job", UUID.randomUUID(), 1L, LocalDateTime.now());

        // FIX: Add PageRequest.of() and total elements
        Page<FeedbackDto> page = new PageImpl<>(List.of(feedback), PageRequest.of(0, 10), 1);

        when(userProfileService.getUserReceivedFeedback(eq(targetUserId), isNull(), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/profile/{userId}/feedback", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].comment").value("Great job"));
    }

    @Test
    void getReceivedFeedback_ShouldReturnEmptyListIfNoFeedbackExists() throws Exception {
        UUID targetUserId = UUID.randomUUID();

        // FIX: Add PageRequest.of() and total elements
        Page<FeedbackDto> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(userProfileService.getUserReceivedFeedback(eq(targetUserId), isNull(), eq(0), eq(10))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/profile/{userId}/feedback", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getGivenFeedback_ShouldReturnFeedbackList() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        FeedbackDto feedback = new FeedbackDto(4, "Good effort", UUID.randomUUID(), 2L, LocalDateTime.now());

        // FIX: Add PageRequest.of() and total elements
        Page<FeedbackDto> page = new PageImpl<>(List.of(feedback), PageRequest.of(0, 10), 1);

        when(userProfileService.getUserGivenFeedback(eq(targetUserId), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/profile/{userId}/feedback/given", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(4));
    }
    @Test
    void getReceivedFeedback_ShouldReturn404IfUserNotFound() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        when(userProfileService.getUserReceivedFeedback(any(), any(), anyInt(), anyInt()))
                .thenThrow(new UserNotFound("User not found"));

        mockMvc.perform(get("/api/profile/{userId}/feedback", targetUserId))
                .andExpect(status().isNotFound());
    }


}