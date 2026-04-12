package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.repository.StudentProjectRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDirectoryServiceTest {

    @Mock
    private StudentProjectRepository studentProjectRepository;
    @Mock
    private StudentCredentialsRepository studentCredentialsRepository;
    @Mock
    private CampusCatalog campusCatalog;

    @Test
    void getProjectNames_shouldReturnDistinctNamesFromRepository() {
        ProjectDirectoryService service = new ProjectDirectoryService(
            studentProjectRepository,
            studentCredentialsRepository,
            campusCatalog
        );
        List<String> expected = List.of("C Piscine C", "CPP Module 00");
        when(studentProjectRepository.findDistinctActualProjectNames()).thenReturn(expected);

        List<String> result = service.getProjectNames();

        assertEquals(expected, result);
        verify(studentProjectRepository).findDistinctActualProjectNames();
    }

    @Test
    void getProjectExecutors_shouldEscapeLikePatternAndFillCampusFromCatalog() {
        ProjectDirectoryService service = new ProjectDirectoryService(
            studentProjectRepository,
            studentCredentialsRepository,
            campusCatalog
        );

        String rawProjectName = "A_100%\\core";
        String escapedProjectName = "A\\_100\\%\\\\core";
        when(studentProjectRepository.findExecutorsByProjectName(escapedProjectName))
            .thenReturn(List.of(new ProjectExecutorDto("login1", null, "IN_PROGRESS", null)));
        when(studentCredentialsRepository.findSchoolIdsByLogins(List.of("login1")))
            .thenReturn(List.of(new StudentCredentialsRepository.LoginSchoolIdView() {
                @Override
                public String getLogin() {
                    return "login1";
                }

                @Override
                public String getSchoolId() {
                    return "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
                }
            }));
        when(campusCatalog.campusName("6bfe3c56-0211-4fe1-9e59-51616caac4dd")).thenReturn("MSK");

        List<ProjectExecutorDto> result = service.getProjectExecutors(rawProjectName);

        assertEquals(1, result.size());
        assertEquals("login1", result.getFirst().login());
        assertEquals("MSK", result.getFirst().campusName());
        verify(studentProjectRepository).findExecutorsByProjectName(escapedProjectName);
    }

    @Test
    void getProjectExecutors_shouldReturnEmptyAndSkipRepository_whenProjectNameBlank() {
        ProjectDirectoryService service = new ProjectDirectoryService(
            studentProjectRepository,
            studentCredentialsRepository,
            campusCatalog
        );

        List<ProjectExecutorDto> result = service.getProjectExecutors("   ");

        assertEquals(List.of(), result);
        verify(studentProjectRepository, never()).findExecutorsByProjectName("   ");
        verify(studentCredentialsRepository, never()).findSchoolIdsByLogins(anyCollection());
    }

    @Test
    void getProjectExecutors_shouldKeepCampusNameFromQuery_whenAlreadyPresent() {
        ProjectDirectoryService service = new ProjectDirectoryService(
            studentProjectRepository,
            studentCredentialsRepository,
            campusCatalog
        );
        when(studentProjectRepository.findExecutorsByProjectName("A1\\_Maze\\_C"))
            .thenReturn(List.of(new ProjectExecutorDto("login2", "MSK", "WAITING_FOR_START", null)));
        when(studentCredentialsRepository.findSchoolIdsByLogins(List.of("login2"))).thenReturn(List.of());

        List<ProjectExecutorDto> result = service.getProjectExecutors("A1_Maze_C");

        assertEquals(1, result.size());
        assertEquals("MSK", result.getFirst().campusName());
    }
}
