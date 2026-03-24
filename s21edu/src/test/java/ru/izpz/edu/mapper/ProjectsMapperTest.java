package ru.izpz.edu.mapper;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.ProjectsDto;
import ru.izpz.edu.dto.StudentProjectData;

import static org.junit.jupiter.api.Assertions.*;

class ProjectsMapperTest {

    private final ProjectsMapper mapper = org.mapstruct.factory.Mappers.getMapper(ProjectsMapper.class);

    @Test
    void toDto_shouldMapFields() {
        StudentProjectData src = new StudentProjectData("g", "n", "d", 1, "dt", 1, 1, "e", "gs", 2, 1);

        ProjectsDto dto = mapper.toDto(src);

        assertEquals("g", dto.goalId());
        assertEquals("n", dto.name());
        assertEquals("d", dto.description());
        assertEquals("e", dto.executionType());
        assertEquals("gs", dto.goalStatus());
        assertEquals(Integer.valueOf(2), dto.amountMembers());
    }
}
