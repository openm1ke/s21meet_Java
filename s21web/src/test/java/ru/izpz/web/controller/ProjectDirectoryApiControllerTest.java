package ru.izpz.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectDirectoryApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectDirectoryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectDirectoryFacade projectDirectoryFacade;

    @MockitoBean
    private TelegramInitDataValidator telegramInitDataValidator;

    @Test
    void getProjectNames_shouldReturnOk() throws Exception {
        when(projectDirectoryFacade.getProjectNames("123456")).thenReturn(List.of("C2_SimpleBashUtils"));

        mockMvc.perform(get("/api/projects/names")
                        .requestAttr("telegramId", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("C2_SimpleBashUtils"));

        verify(projectDirectoryFacade).getProjectNames("123456");
    }

    @Test
    void getProjectNames_shouldReturnUnauthorized_whenTelegramIdMissing() throws Exception {
        mockMvc.perform(get("/api/projects/names"))
                .andExpect(status().isUnauthorized());

        verify(projectDirectoryFacade, never()).getProjectNames(anyString());
    }

    @Test
    void getProjectNames_shouldReturnUnauthorized_whenTelegramIdBlank() throws Exception {
        mockMvc.perform(get("/api/projects/names")
                        .requestAttr("telegramId", "   "))
                .andExpect(status().isUnauthorized());

        verify(projectDirectoryFacade, never()).getProjectNames(anyString());
    }

    @Test
    void getProjectNames_shouldReturnAllWhenAllFlagEnabled() throws Exception {
        when(projectDirectoryFacade.getAllProjectNames()).thenReturn(List.of("A1_Maze_C", "C2_SimpleBashUtils"));

        mockMvc.perform(get("/api/projects/names").param("all", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("A1_Maze_C"))
                .andExpect(jsonPath("$[1]").value("C2_SimpleBashUtils"));

        verify(projectDirectoryFacade).getAllProjectNames();
        verify(projectDirectoryFacade, never()).getProjectNames(anyString());
    }

    @Test
    void getProjectExecutors_shouldReturnOk() throws Exception {
        ProjectExecutorsRequest request = new ProjectExecutorsRequest("C2_SimpleBashUtils");
        when(projectDirectoryFacade.getProjectExecutors(request))
                .thenReturn(List.of(new ProjectExecutorDto("mike", "Kazan", "IN_PROGRESS", null)));

        mockMvc.perform(post("/api/projects/executors")
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

        mockMvc.perform(post("/api/projects/executors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(projectDirectoryFacade, never()).getProjectExecutors(any(ProjectExecutorsRequest.class));
    }

    @Test
    void getProjectExecutors_shouldReturnBadRequest_whenProjectNameTooLong() throws Exception {
        ProjectExecutorsRequest request = new ProjectExecutorsRequest("a".repeat(121));

        mockMvc.perform(post("/api/projects/executors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(projectDirectoryFacade, never()).getProjectExecutors(any(ProjectExecutorsRequest.class));
    }
}
