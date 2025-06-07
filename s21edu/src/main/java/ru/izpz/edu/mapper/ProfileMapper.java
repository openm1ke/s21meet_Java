package ru.izpz.edu.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.izpz.dto.ProfileDto;
import ru.izpz.edu.model.Profile;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    ProfileDto toDto(Profile profile);
    @Mapping(target = "id", ignore = true)
    Profile toEntity(ProfileDto dto);
}
