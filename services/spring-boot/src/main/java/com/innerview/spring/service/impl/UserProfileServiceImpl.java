package com.innerview.spring.service.impl;

import com.innerview.spring.dto.CreateProfileRequest;
import com.innerview.spring.dto.UpdateImageRequest;
import com.innerview.spring.dto.UserAverageRatingProjection;
import com.innerview.spring.dto.UserAverageRatingResponse;
import com.innerview.spring.entity.UserProfile;
import com.innerview.spring.exception.UserHasProfile;
import com.innerview.spring.exception.UserNotFound;
import com.innerview.spring.exception.UserProfileNotFound;
import com.innerview.spring.mapper.UserProfileMapper;
import com.innerview.spring.repository.UserProfileRepository;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final UserRepository userRepository;
    @Override
    public UserProfile createProfile(UUID currentUserID, CreateProfileRequest createProfileRequest){

        var user=userRepository.findUserById(currentUserID);

        //user is not registered
        if(user.isEmpty()){
            throw  new UserNotFound("user not found");
        }
        //conflict case
        if(userProfileRepository.getUserProfileByUser_Id(currentUserID)!=null){
            throw new UserHasProfile("user already has a profile");
        }
        var userProfile= userProfileMapper.toEntity(createProfileRequest);

        userProfileRepository.save(userProfile);

        return userProfile;
    }
    public UserProfile findProfileById(@AuthenticationPrincipal UUID currentUserId){
        UserProfile userProfile = userProfileRepository.getUserProfileByUser_Id(currentUserId);
        if(userProfile==null){
            throw  new UserProfileNotFound("User profile not found");
        }
        return userProfile;
    }

    public UserProfile UpdateProfile(UUID currentUserID, CreateProfileRequest createProfileRequest){
        UserProfile userProfile = userProfileRepository.getUserProfileByUser_Id(currentUserID);
        if(userProfile==null){
            throw new UserProfileNotFound("User profile does not exist for this user");
        }

        userProfileMapper.update(createProfileRequest,userProfile);
        userProfileRepository.save(userProfile);
        return  userProfile;
    }

    @Override
    public void deleteUserProfileById(UUID currentUserID) {
        UserProfile userProfile=userProfileRepository.getUserProfileByUser_Id(currentUserID);
        if(userProfile==null){
            throw  new UserProfileNotFound("User profile not found");
        }
        userProfileRepository.delete(userProfile);
    }

    @Override
    public void changeUserProfilePhoto(UUID currentUserId, UpdateImageRequest updateImageRequest) {
        UserProfile userProfile=userProfileRepository.getUserProfileByUser_Id(currentUserId);
        if(userProfile==null){
            throw  new UserProfileNotFound("User profile not found");
        }
        userProfile.setImageUrl(updateImageRequest.getPhotoUrl());
    }

    @Override
    public UserAverageRatingResponse getAverageRatingById(UUID currentUserId) {
        UserProfile userProfile=userProfileRepository.getUserProfileByUser_Id(currentUserId);
        if(userProfile==null){
            throw  new UserProfileNotFound("User profile not found");
        }

       UserAverageRatingProjection userAverageRatingProjection = userProfileRepository.getUserAverageRating(currentUserId);
       return new UserAverageRatingResponse(currentUserId,userAverageRatingProjection.getAverageRating(),userAverageRatingProjection.getTotalReviews());
    }



}
