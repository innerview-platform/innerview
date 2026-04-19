package com.innerview.spring.controller;

import com.innerview.spring.dto.CreateProfileRequest;
import com.innerview.spring.dto.UpdateImageRequest;
import com.innerview.spring.dto.UserAverageRatingResponse;
import com.innerview.spring.entity.UserProfile;
import com.innerview.spring.service.impl.UserProfileServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RequestMapping("/api/profile")
public class UserProfileController {

    final private UserProfileServiceImpl userProfileService;

    @PostMapping
    public ResponseEntity<UserProfile> createProfile(@AuthenticationPrincipal UUID currentUserId, @Valid @RequestBody CreateProfileRequest createProfileRequest) {
        UserProfile userProfile = userProfileService.createProfile(currentUserId, createProfileRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfile);
    }

    @GetMapping
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal UUID currentUserId)
    {
         UserProfile userProfile=userProfileService.findProfileById(currentUserId);
         return ResponseEntity.status(HttpStatus.OK).body(userProfile);

    }

    @PutMapping
    public ResponseEntity<UserProfile> updateUserProfile(@AuthenticationPrincipal UUID currentUserId, @Valid @RequestBody CreateProfileRequest createProfileRequest){
        UserProfile userProfile=userProfileService.UpdateProfile(currentUserId,createProfileRequest);
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












}
