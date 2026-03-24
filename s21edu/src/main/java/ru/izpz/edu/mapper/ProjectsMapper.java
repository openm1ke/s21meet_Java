package ru.izpz.edu.mapper;

import org.mapstruct.Mapper;
import ru.izpz.dto.ProjectsDto;
import ru.izpz.edu.dto.StudentProjectData;

@Mapper(componentModel = "spring")
public interface ProjectsMapper {
    ProjectsDto toDto(StudentProjectData projects);
}
