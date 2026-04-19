package com.innerview.spring.mapper;

import com.innerview.spring.dto.CreateProfileRequest;
import com.innerview.spring.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.TargetType;

import java.lang.annotation.Target;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserProfileMapper {

    CreateProfileRequest toDto(UserProfile userProfile);

    UserProfile toEntity(CreateProfileRequest createProfileRequest);

    void update(CreateProfileRequest createProfileRequest, @MappingTarget UserProfile userProfile);

}
