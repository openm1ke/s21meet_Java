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
    @Mapping(target = "coalition", ignore = true)
    @Mapping(target = "isOnline", ignore = true)
    @Mapping(target = "seat", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    ParticipantDto toDto(Participant entity);
    default ParticipantDto toDto(ParticipantV1DTO dto) {
        if (dto == null) {
            return null;
        }
        ParticipantDto result = new ParticipantDto();
        result.setLogin(dto.getLogin());
        result.setClassName(dto.getClassName());
        result.setParallelName(dto.getParallelName());
        result.setExpValue(Math.toIntExact(dto.getExpValue()));
        result.setLevel(dto.getLevel());
        result.setExpToNextLevel(Math.toIntExact(dto.getExpToNextLevel()));
        result.setStatus(ru.izpz.dto.ParticipantStatusEnum.valueOf(dto.getStatus().name()));
        result.setCampus(toDto(dto.getCampus()));
        return result;
    }

    @Mapping(target = "campusName", source = "shortName")
    ParticipantCampus toEntity(ParticipantCampusV1DTO dto);
    ParticipantCampusDto toDto(ParticipantCampus entity);
    default ParticipantCampusDto toDto(ParticipantCampusV1DTO dto) {
        if (dto == null) {
            return null;
        }
        ParticipantCampusDto campusDto = new ParticipantCampusDto();
        campusDto.setId(dto.getId().toString());
        campusDto.setCampusName(dto.getShortName());
        return campusDto;
    }
}
