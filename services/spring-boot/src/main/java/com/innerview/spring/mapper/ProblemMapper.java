package com.innerview.spring.mapper;

import com.innerview.spring.dto.*;
import com.innerview.spring.entity.Problem;
import com.innerview.spring.entity.TestCase;
import com.innerview.spring.entity.User;
import org.mapstruct.*;

@Mapper
public interface ProblemMapper {

    ProblemResponseDTO toResponseDTO(Problem problem);

    ProblemOwnerDTO toOwnerDTO(Problem problem);

    ProblemCreatorDTO toCreatorDTO(User user);
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active",  ignore = true)
    Problem toEntity(CreateProblemRequest request);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateProblemRequest request, @MappingTarget Problem problem);
}
