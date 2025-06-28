package ru.izpz.edu.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.izpz.dto.ParticipantCampusDto;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.model.ParticipantCampusV1DTO;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.model.Participant;
import ru.izpz.edu.model.ParticipantCampus;
import ru.izpz.edu.model.Profile;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    ProfileDto toDto(Profile profile);
    @Mapping(target = "id", ignore = true)
    Profile toEntity(ProfileDto dto);

    Participant toEntity(ParticipantV1DTO dto);

    ParticipantDto toDto(Participant entity);
    @Mapping(target = "campusName", source = "shortName")
    ParticipantCampus toEntity(ParticipantCampusV1DTO dto);

    ParticipantCampusDto toDto(ParticipantCampus entity);
}
