package com.innerview.spring.service.impl;

import com.innerview.spring.dto.*;
import com.innerview.spring.entity.UserProfile;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.exception.UserHasProfile;
import com.innerview.spring.exception.UserNotFound;
import com.innerview.spring.exception.UserProfileNotFound;
import com.innerview.spring.mapper.UserProfileMapper;
import com.innerview.spring.repository.FeedbackRepository;
import com.innerview.spring.repository.UserInterviewRepository;
import com.innerview.spring.repository.UserProfileRepository;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final UserRepository userRepository;
    private final UserInterviewRepository userInterviewRepository;
    private final FeedbackRepository feedbackRepository;


    public UserProfile getUserProfile(UUID currentUserID ){
        Optional<UserProfile> userProfile=userProfileRepository.getUserProfileByUser_Id(currentUserID);
        if(userProfile.isEmpty()){
            throw  new UserProfileNotFound("User profile not found");
        }

        return userProfile.get();

    }
    @Override
    public UserProfileResponse createProfile(UUID currentUserID, CreateProfileRequest createProfileRequest){

        var user=userRepository.findUserById(currentUserID);

        //user is not registered
        if(user.isEmpty()){
            throw  new UserNotFound("user not found");
        }
        //conflict case
        if(userProfileRepository.getUserProfileByUser_Id(currentUserID).isPresent()){
            throw new UserHasProfile("user already has a profile");
        }
        var userProfile= userProfileMapper.toEntity(createProfileRequest);

        userProfile.setUser(user.get());

        userProfileRepository.save(userProfile);

        return userProfileMapper.toResponse(userProfile);
    }
    @Override
    public UserProfileResponse findProfileById(@AuthenticationPrincipal UUID currentUserId){
        UserProfile userProfile=getUserProfile(currentUserId);
        return userProfileMapper.toResponse(userProfile);
    }

    @Override
    public UserProfileResponse UpdateProfile(UUID currentUserID, CreateProfileRequest createProfileRequest){
        UserProfile userProfile=getUserProfile(currentUserID);
        userProfileMapper.update(createProfileRequest,userProfile);
        userProfileRepository.save(userProfile);
        return userProfileMapper.toResponse(userProfile);
    }

    @Override
    public void deleteUserProfileById(UUID currentUserID) {
        UserProfile userProfile=getUserProfile(currentUserID);
        userProfileRepository.delete(userProfile);
    }

    @Override
    public void changeUserProfilePhoto(UUID currentUserId, UpdateImageRequest updateImageRequest) {
        UserProfile userProfile=getUserProfile(currentUserId);
        userProfile.setImageUrl(updateImageRequest.getPhotoUrl());
    }

    @Override
    public UserAverageRatingResponse getAverageRatingById(UUID currentUserId) {
        UserProfile userProfile=getUserProfile(currentUserId);
        UserAverageRatingProjection userAverageRatingProjection = userProfileRepository.getUserAverageRating(currentUserId);

        return  new UserAverageRatingResponse(currentUserId,userAverageRatingProjection.getAverageRating(),userAverageRatingProjection.getTotalReviews());
    }

    @Override
    public Page<InterviewHistoryDto> getUserInterviewHistory(
            UUID userId,
            InterviewStatus status,
            InterviewType type,
            int page,
            int limit) {

        getUserProfile(userId);

        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "interview.startTime"));

        return userInterviewRepository.findInterviewHistoryByUser(userId, status, type, pageable);
    }
   @Override
    public Page<FeedbackDto> getUserReceivedFeedback(UUID userId, Integer rating, int page, int limit) {
        getUserProfile(userId);
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return feedbackRepository.findFeedbackReceivedByUser(userId, rating, pageable);
    }

    @Override
    public Page<FeedbackDto> getUserGivenFeedback(UUID userId, int page, int limit) {
        getUserProfile(userId);
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return feedbackRepository.findFeedbackGivenByUser(userId, pageable);
    }





}
