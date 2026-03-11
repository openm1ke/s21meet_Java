package ru.izpz.edu.mapper;

import org.mapstruct.Mapper;
import ru.izpz.dto.ProfileCodeResponse;
import ru.izpz.edu.model.ProfileValidation;

@Mapper(componentModel = "spring")
public interface ProfileVerificationMapper {

    ProfileCodeResponse toProfileCodeResponse(ProfileValidation save);
}
