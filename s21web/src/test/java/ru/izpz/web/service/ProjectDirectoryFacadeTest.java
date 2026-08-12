package ru.izpz.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.izpz.dto.CampusRequest;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.client.EduProfileClient;

@ExtendWith(MockitoExtension.class)
class ProjectDirectoryFacadeTest {
  private static final String PROJECT_NAME = "A1_Maze_C";

  @Mock private EduProfileClient eduProfileClient;

  @Test
  void getProjectNames_shouldDelegateToEduClient() {
    ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
    when(eduProfileClient.getProjectNames(any(CampusRequest.class)))
        .thenReturn(List.of(PROJECT_NAME));

    List<String> result = facade.getProjectNames("123456");

    assertEquals(List.of(PROJECT_NAME), result);
    verify(eduProfileClient)
        .getProjectNames(argThat(req -> req != null && "123456".equals(req.getTelegramId())));
  }

  @Test
  void getProjectNames_shouldPropagateEduClientFailure() {
    ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
    RuntimeException failure = new RuntimeException("profile service unavailable");
    when(eduProfileClient.getProjectNames(any(CampusRequest.class))).thenThrow(failure);

    assertEquals(
        failure, assertThrows(RuntimeException.class, () -> facade.getProjectNames("1234")));
  }

  @Test
  void getProjectExecutors_shouldDelegateWithRequest() {
    ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
    when(eduProfileClient.getProjectExecutors(any(ProjectExecutorsRequest.class)))
        .thenReturn(List.of(new ProjectExecutorDto("mike", "MSK", "IN_PROGRESS", null)));

    ProjectExecutorsRequest request = new ProjectExecutorsRequest(PROJECT_NAME);
    List<ProjectExecutorDto> result = facade.getProjectExecutors(request);

    assertEquals(1, result.size());
    assertEquals("mike", result.getFirst().login());
    verify(eduProfileClient).getProjectExecutors(new ProjectExecutorsRequest(PROJECT_NAME));
  }

  @Test
  void getAllProjectNames_shouldDelegateToEduClient() {
    ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
    when(eduProfileClient.getAllProjectNames())
        .thenReturn(List.of("A1_Maze_C", "C2_SimpleBashUtils"));

    List<String> result = facade.getAllProjectNames();

    assertEquals(List.of("A1_Maze_C", "C2_SimpleBashUtils"), result);
    verify(eduProfileClient).getAllProjectNames();
  }

  @Test
  void getAllProjectNames_shouldPropagateEduClientFailure() {
    ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
    RuntimeException failure = new RuntimeException("profile service unavailable");
    when(eduProfileClient.getAllProjectNames()).thenThrow(failure);

    assertEquals(failure, assertThrows(RuntimeException.class, facade::getAllProjectNames));
  }

  @Test
  void fallbackMethods_shouldReturnEmptyLists() {
    ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);

    List<String> names =
        ReflectionTestUtils.invokeMethod(
            facade, "fallbackProjectNames", "123456", new RuntimeException("down"));
    List<String> allNames =
        ReflectionTestUtils.invokeMethod(
            facade, "fallbackAllProjectNames", new RuntimeException("down"));
    List<ProjectExecutorDto> executors =
        ReflectionTestUtils.invokeMethod(
            facade,
            "fallbackProjectExecutors",
            new ProjectExecutorsRequest(PROJECT_NAME),
            new RuntimeException("down"));
    List<String> namesWithBlankTelegramId =
        ReflectionTestUtils.invokeMethod(
            facade, "fallbackProjectNames", " ", new RuntimeException("down"));
    List<String> namesWithNullTelegramId =
        ReflectionTestUtils.invokeMethod(
            facade, "fallbackProjectNames", null, new RuntimeException("down"));
    List<ProjectExecutorDto> executorsWithNullRequest =
        ReflectionTestUtils.invokeMethod(
            facade, "fallbackProjectExecutors", null, new RuntimeException("down"));

    assertTrue(names.isEmpty());
    assertTrue(allNames.isEmpty());
    assertTrue(executors.isEmpty());
    assertTrue(namesWithBlankTelegramId.isEmpty());
    assertTrue(namesWithNullTelegramId.isEmpty());
    assertTrue(executorsWithNullRequest.isEmpty());
  }
}
