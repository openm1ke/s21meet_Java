package ru.school21.edu.mapper;

import org.mapstruct.Mapper;
import ru.school21.edu.model.Campus;
import ru.school21.edu.model.CampusV1DTO;

@Mapper(componentModel = "spring")
public interface CampusMapper {
    Campus toEntity(CampusV1DTO dto);
}
