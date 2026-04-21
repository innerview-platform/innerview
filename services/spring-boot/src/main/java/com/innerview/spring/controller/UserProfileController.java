package com.innerview.spring.controller;

import com.innerview.spring.dto.*;
import com.innerview.spring.entity.UserProfile;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.service.UserProfileService;
import com.innerview.spring.service.impl.UserProfileServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(@AuthenticationPrincipal UUID currentUserId, @Valid @RequestBody CreateProfileRequest createProfileRequest) {
        UserProfileResponse userProfile = userProfileService.createProfile(currentUserId, createProfileRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfile);
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UUID currentUserId)
    {
        UserProfileResponse userProfile=userProfileService.findProfileById(currentUserId);
         return ResponseEntity.status(HttpStatus.OK).body(userProfile);

    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateUserProfile(@AuthenticationPrincipal UUID currentUserId, @Valid @RequestBody CreateProfileRequest createProfileRequest){
        UserProfileResponse userProfile=userProfileService.UpdateProfile(currentUserId,createProfileRequest);
        return ResponseEntity.status(HttpStatus.OK).body(userProfile);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteUserProfile(@AuthenticationPrincipal UUID currentUserId){
        userProfileService.deleteUserProfileById(currentUserId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/image")
    public ResponseEntity<?> updateImage(@AuthenticationPrincipal UUID currentUserId,@RequestBody UpdateImageRequest updateImageRequest){
        userProfileService.changeUserProfilePhoto(currentUserId,updateImageRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("{userId}/rating")
    public ResponseEntity<UserAverageRatingResponse> getUserAverageRating(@PathVariable UUID userId){
        UserAverageRatingResponse userAverageRatingResponse=userProfileService.getAverageRatingById(userId);
        return  ResponseEntity.status(HttpStatus.OK).body(userAverageRatingResponse);
    }

    @GetMapping("/{userId}/interviews")
    public ResponseEntity<Page<InterviewHistoryDto>> getUserInterviews(
            @PathVariable UUID userId,
            @RequestParam(required = false) InterviewStatus status,
            @RequestParam(required = false) InterviewType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<InterviewHistoryDto> history = userProfileService.getUserInterviewHistory(
                userId, status, type, page, limit
        );

        return ResponseEntity.ok(history);
    }

    @GetMapping("/{userId}/feedback")
    public ResponseEntity<Page<FeedbackDto>> getReceivedFeedback(
            @PathVariable UUID userId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<FeedbackDto> feedback = userProfileService.getUserReceivedFeedback(userId, rating, page, limit);
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/{userId}/feedback/given")
    public ResponseEntity<Page<FeedbackDto>> getGivenFeedback(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<FeedbackDto> feedback = userProfileService.getUserGivenFeedback(userId, page, limit);
        return ResponseEntity.ok(feedback);
    }












}
