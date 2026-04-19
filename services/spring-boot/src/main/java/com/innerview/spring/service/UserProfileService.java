package com.innerview.spring.service;

import com.innerview.spring.dto.CreateProfileRequest;
import com.innerview.spring.dto.UpdateImageRequest;
import com.innerview.spring.dto.UserAverageRatingResponse;
import com.innerview.spring.entity.UserProfile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

public interface UserProfileService {
    UserProfile createProfile(UUID currentUserId, CreateProfileRequest createProfileRequest);
    UserProfile findProfileById(@AuthenticationPrincipal UUID currentUserId);
    UserProfile UpdateProfile(UUID currentUserID, CreateProfileRequest createProfileRequest);
    void deleteUserProfileById(UUID currentUserID);
    void changeUserProfilePhoto(@AuthenticationPrincipal UUID currentUserId, @RequestBody UpdateImageRequest updateImageRequest);
    UserAverageRatingResponse getAverageRatingById(@AuthenticationPrincipal UUID currentUserId);
}
