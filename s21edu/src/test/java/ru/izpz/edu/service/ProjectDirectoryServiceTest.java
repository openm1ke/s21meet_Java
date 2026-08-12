package ru.izpz.edu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsPageRequest;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.repository.StudentProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectDirectoryServiceTest {

  @Mock private StudentProjectRepository studentProjectRepository;
  @Mock private StudentCredentialsRepository studentCredentialsRepository;
  @Mock private CampusCatalog campusCatalog;

  @Test
  void getProjectNames_shouldReturnDistinctNamesFromRepository() {
    ProjectDirectoryService service =
        new ProjectDirectoryService(
            studentProjectRepository, studentCredentialsRepository, campusCatalog);
    List<String> expected = List.of("C Piscine C", "CPP Module 00");
    when(studentProjectRepository.findDistinctActualProjectNames()).thenReturn(expected);

    List<String> result = service.getProjectNames();

    assertEquals(expected, result);
    verify(studentProjectRepository).findDistinctActualProjectNames();
  }

  @Test
  void getProjectExecutors_shouldEscapeLikePatternAndFillCampusFromCatalog() {
    ProjectDirectoryService service =
        new ProjectDirectoryService(
            studentProjectRepository, studentCredentialsRepository, campusCatalog);

    String rawProjectName = "A_100%\\core";
    String escapedProjectName = "A\\_100\\%\\\\core";
    when(studentProjectRepository.findExecutorsByProjectName(escapedProjectName))
        .thenReturn(List.of(new ProjectExecutorDto("login1", null, "IN_PROGRESS", null, null)));
    when(studentCredentialsRepository.findSchoolIdsByLogins(List.of("login1")))
        .thenReturn(
            List.of(
                new StudentCredentialsRepository.LoginSchoolIdView() {
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

    List<ProjectExecutorDto> result =
        service.getProjectExecutors(new ProjectExecutorsRequest(rawProjectName));

    assertEquals(1, result.size());
    assertEquals("login1", result.getFirst().login());
    assertEquals("MSK", result.getFirst().campusName());
    verify(studentProjectRepository).findExecutorsByProjectName(escapedProjectName);
  }

  @Test
  void getProjectExecutors_shouldReturnEmptyAndSkipRepository_whenProjectNameBlank() {
    ProjectDirectoryService service =
        new ProjectDirectoryService(
            studentProjectRepository, studentCredentialsRepository, campusCatalog);

    List<ProjectExecutorDto> result =
        service.getProjectExecutors(new ProjectExecutorsRequest("   "));

    assertEquals(List.of(), result);
    verify(studentProjectRepository, never()).findExecutorsByProjectName(anyString());
    verify(studentCredentialsRepository, never()).findSchoolIdsByLogins(anyCollection());
  }

  @Test
  void getProjectExecutors_shouldKeepCampusNameFromQuery_whenAlreadyPresent() {
    ProjectDirectoryService service =
        new ProjectDirectoryService(
            studentProjectRepository, studentCredentialsRepository, campusCatalog);
    when(studentProjectRepository.findExecutorsByProjectName("A1\\_Maze\\_C"))
        .thenReturn(
            List.of(
                new ProjectExecutorDto(
                    "login2", "MSK", "WAITING_FOR_START", null, "22_10_MSK")));
    when(studentCredentialsRepository.findSchoolIdsByLogins(List.of("login2")))
        .thenReturn(List.of());

    List<ProjectExecutorDto> result =
        service.getProjectExecutors(new ProjectExecutorsRequest("A1_Maze_C"));

    assertEquals(1, result.size());
    assertEquals("MSK", result.getFirst().campusName());
  }

  @Test
  void getProjectExecutors_shouldNormalizeCampusNameToThreeLetterCode() {
    ProjectDirectoryService service =
        new ProjectDirectoryService(
            studentProjectRepository, studentCredentialsRepository, campusCatalog);
    when(studentProjectRepository.findExecutorsByProjectName("A1\\_Maze\\_C"))
        .thenReturn(
            List.of(
                new ProjectExecutorDto(
                    "login2", "21 Moscow", "WAITING_FOR_START", null, "22_10_MSK")));
    when(studentCredentialsRepository.findSchoolIdsByLogins(List.of("login2")))
        .thenReturn(List.of());

    List<ProjectExecutorDto> result =
        service.getProjectExecutors(new ProjectExecutorsRequest("A1_Maze_C"));

    assertEquals(1, result.size());
    assertEquals("MSK", result.getFirst().campusName());
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 20, 50})
  void getProjectExecutorsPage_shouldRespectPageSizeAndReturnTotals(int size) {
    ProjectDirectoryService service =
        new ProjectDirectoryService(
            studentProjectRepository, studentCredentialsRepository, campusCatalog);
    List<ProjectExecutorDto> rows =
        IntStream.range(0, Math.min(size, 3))
            .mapToObj(i -> new ProjectExecutorDto("login" + i, "MSK", "IN_PROGRESS", null, null))
            .toList();
    when(studentProjectRepository.findExecutorsByProjectNamePaged(
            org.mockito.ArgumentMatchers.eq("A1\\_Maze\\_C"),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(rows, Pageable.ofSize(size), 1333));
    when(studentProjectRepository.findDistinctCampusesByProjectName("A1\\_Maze\\_C"))
        .thenReturn(List.of("MSK"));
    when(studentProjectRepository.findDistinctStatusesByProjectName("A1\\_Maze\\_C"))
        .thenReturn(List.of("IN_PROGRESS"));

    var result =
        service.getProjectExecutorsPage(
            new ProjectExecutorsPageRequest("A1_Maze_C", 0, size, List.of(), List.of(), "asc"));

    assertEquals(rows, result.items());
    assertEquals(size, result.size());
    assertEquals(1333, result.totalItems());
    assertEquals((int) Math.ceil(1333.0 / size), result.totalPages());
    assertEquals(List.of("MSK"), result.availableCampuses());
  }

  @Test
  void getProjectExecutorsPage_shouldApplyFiltersAndEscapeProjectName() {
    ProjectDirectoryService service =
        new ProjectDirectoryService(
            studentProjectRepository, studentCredentialsRepository, campusCatalog);
    when(studentProjectRepository.findExecutorsByProjectNamePaged(
            org.mockito.ArgumentMatchers.eq("A\\_100\\%\\\\core"),
            org.mockito.ArgumentMatchers.eq(List.of("MSK")),
            org.mockito.ArgumentMatchers.eq(false),
            org.mockito.ArgumentMatchers.eq(List.of("IN_PROGRESS")),
            org.mockito.ArgumentMatchers.eq(false),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));
    when(studentProjectRepository.findDistinctCampusesByProjectName("A\\_100\\%\\\\core"))
        .thenReturn(List.of());
    when(studentProjectRepository.findDistinctStatusesByProjectName("A\\_100\\%\\\\core"))
        .thenReturn(List.of());

    var result =
        service.getProjectExecutorsPage(
            new ProjectExecutorsPageRequest(
                "A_100%\\core", 0, 20, List.of(" msk "), List.of("IN_PROGRESS"), "invalid"));

    assertEquals(0, result.totalItems());
    verify(studentProjectRepository)
        .findExecutorsByProjectNamePaged(
            org.mockito.ArgumentMatchers.eq("A\\_100\\%\\\\core"),
            org.mockito.ArgumentMatchers.eq(List.of("MSK")),
            org.mockito.ArgumentMatchers.eq(false),
            org.mockito.ArgumentMatchers.eq(List.of("IN_PROGRESS")),
            org.mockito.ArgumentMatchers.eq(false),
            org.mockito.ArgumentMatchers.any(Pageable.class));
  }
}
