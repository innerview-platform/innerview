package com.innerview.spring.mapper;

import com.innerview.spring.dto.TestCaseDto;
import com.innerview.spring.entity.TestCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TestCaseMapper {

    TestCaseDto toTestCaseDto(TestCase testCase);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "problem", ignore = true)
    TestCase toEntity(TestCaseDto request);

    // NEW: Updates an existing entity from a DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "problem", ignore = true)
    void updateEntityFromDto(TestCaseDto request, @MappingTarget TestCase existingTestCase);
}