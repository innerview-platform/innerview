package com.innerview.spring.service;

import com.innerview.spring.dto.*;
import com.innerview.spring.entity.UserProfile;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

public interface UserProfileService {
    UserProfileResponse createProfile(UUID currentUserId, CreateProfileRequest createProfileRequest);
    UserProfileResponse findProfileById(@AuthenticationPrincipal UUID currentUserId);
    UserProfileResponse UpdateProfile(UUID currentUserID, CreateProfileRequest createProfileRequest);
    void deleteUserProfileById(UUID currentUserID);
    void changeUserProfilePhoto(@AuthenticationPrincipal UUID currentUserId, @RequestBody UpdateImageRequest updateImageRequest);
    UserAverageRatingResponse getAverageRatingById(@AuthenticationPrincipal UUID currentUserId);
    Page<InterviewHistoryDto> getUserInterviewHistory(
            UUID userId,
            InterviewStatus status,
            InterviewType type,
            int page,
            int limit);
    Page<FeedbackDto> getUserGivenFeedback(UUID userId, int page, int limit);
    Page<FeedbackDto> getUserReceivedFeedback(UUID userId, Integer rating, int page, int limit);
    UserProfile getUserProfile(UUID currentUserID );
}
