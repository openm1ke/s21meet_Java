package ru.izpz.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.security.TelegramInitDataValidator;
import ru.izpz.web.service.ProjectDirectoryFacade;

@WebMvcTest(ProjectDirectoryApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectDirectoryApiControllerTest {
  private static final String TELEGRAM_ID = "123456";
  private static final String PROJECT_NAME = "C2_SimpleBashUtils";
  private static final String PROJECT_NAMES_PATH = "/api/projects/names";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ProjectDirectoryFacade projectDirectoryFacade;

  @MockitoBean private TelegramInitDataValidator telegramInitDataValidator;

  @Test
  void getProjectNames_shouldReturnOk() throws Exception {
    when(projectDirectoryFacade.getProjectNames(TELEGRAM_ID)).thenReturn(List.of(PROJECT_NAME));

    mockMvc
        .perform(get(PROJECT_NAMES_PATH).requestAttr("telegramId", TELEGRAM_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value(PROJECT_NAME));

    verify(projectDirectoryFacade).getProjectNames(TELEGRAM_ID);
  }

  @Test
  void getProjectNames_shouldReturnOkForShortTelegramId() throws Exception {
    when(projectDirectoryFacade.getProjectNames("1234")).thenReturn(List.of(PROJECT_NAME));

    mockMvc
        .perform(get(PROJECT_NAMES_PATH).requestAttr("telegramId", "1234"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value(PROJECT_NAME));

    verify(projectDirectoryFacade).getProjectNames("1234");
  }

  @Test
  void getProjectNames_shouldReturnUnauthorized_whenTelegramIdMissing() throws Exception {
    mockMvc.perform(get(PROJECT_NAMES_PATH)).andExpect(status().isUnauthorized());

    verify(projectDirectoryFacade, never()).getProjectNames(anyString());
  }

  @Test
  void getProjectNames_shouldReturnUnauthorized_whenTelegramIdBlank() throws Exception {
    mockMvc
        .perform(get(PROJECT_NAMES_PATH).requestAttr("telegramId", "   "))
        .andExpect(status().isUnauthorized());

    verify(projectDirectoryFacade, never()).getProjectNames(anyString());
  }

  @Test
  void getProjectNames_shouldReturnAllWhenAllFlagEnabled() throws Exception {
    when(projectDirectoryFacade.getAllProjectNames())
        .thenReturn(List.of("A1_Maze_C", "C2_SimpleBashUtils"));

    mockMvc
        .perform(get(PROJECT_NAMES_PATH).param("all", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("A1_Maze_C"))
        .andExpect(jsonPath("$[1]").value("C2_SimpleBashUtils"));

    verify(projectDirectoryFacade).getAllProjectNames();
    verify(projectDirectoryFacade, never()).getProjectNames(anyString());
  }

  @Test
  void getProjectExecutors_shouldReturnOk() throws Exception {
    ProjectExecutorsRequest request = new ProjectExecutorsRequest(PROJECT_NAME);
    when(projectDirectoryFacade.getProjectExecutors(request))
        .thenReturn(List.of(new ProjectExecutorDto("mike", "Kazan", "IN_PROGRESS", null)));

    mockMvc
        .perform(
            post("/api/projects/executors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].login").value("mike"))
        .andExpect(jsonPath("$[0].campusName").value("Kazan"))
        .andExpect(jsonPath("$[0].projectStatus").value("IN_PROGRESS"));

    verify(projectDirectoryFacade).getProjectExecutors(request);
  }

  @Test
  void getProjectExecutors_shouldReturnBadRequest_whenProjectNameBlank() throws Exception {
    ProjectExecutorsRequest request = new ProjectExecutorsRequest(" ");

    mockMvc
        .perform(
            post("/api/projects/executors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(projectDirectoryFacade, never()).getProjectExecutors(any(ProjectExecutorsRequest.class));
  }

  @Test
  void getProjectExecutors_shouldReturnBadRequest_whenProjectNameTooLong() throws Exception {
    ProjectExecutorsRequest request = new ProjectExecutorsRequest("a".repeat(121));

    mockMvc
        .perform(
            post("/api/projects/executors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(projectDirectoryFacade, never()).getProjectExecutors(any(ProjectExecutorsRequest.class));
  }
}
