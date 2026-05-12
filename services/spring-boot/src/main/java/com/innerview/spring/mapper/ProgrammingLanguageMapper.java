package com.innerview.spring.mapper;

import com.innerview.spring.dto.CreateProgrammingLanguageRequest;
import com.innerview.spring.dto.ProgrammingLanguageDto;
import com.innerview.spring.entity.ProgrammingLanguage;
import com.innerview.spring.entity.User;
import com.innerview.spring.entity.UserLanguage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProgrammingLanguageMapper {

    ProgrammingLanguageDto toDto(ProgrammingLanguage language);

    @Mapping(source = "language.id", target = "id")
    @Mapping(source = "language.name", target = "name")
    ProgrammingLanguageDto toDto(UserLanguage userLanguage);

    List<ProgrammingLanguageDto> toDtoList(List<UserLanguage> userLanguages);

    ProgrammingLanguage toEntity(CreateProgrammingLanguageRequest request);

    @Mapping(source = "user", target = "user")
    @Mapping(source = "language", target = "language")
    @Mapping(target = "id", ignore = true)
    UserLanguage toEntity(User user, ProgrammingLanguage language);
}
