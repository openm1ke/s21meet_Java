package ru.izpz.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.service.ProjectDirectoryFacade;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectDirectoryApiController.class)
class ProjectDirectoryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectDirectoryFacade projectDirectoryFacade;

    @Test
    void getProjectExecutors_shouldReturnOk() throws Exception {
        when(projectDirectoryFacade.getProjectExecutors("C2_SimpleBashUtils"))
                .thenReturn(List.of(new ProjectExecutorDto("mike", "Kazan", "IN_PROGRESS", "cluster=11, row=A, place=5")));

        ProjectExecutorsRequest request = new ProjectExecutorsRequest("C2_SimpleBashUtils");

        mockMvc.perform(post("/api/projects/executors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("mike"))
                .andExpect(jsonPath("$[0].campusName").value("Kazan"))
                .andExpect(jsonPath("$[0].projectStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].campusPlace").value("cluster=11, row=A, place=5"));

        verify(projectDirectoryFacade).getProjectExecutors("C2_SimpleBashUtils");
    }

    @Test
    void getProjectExecutors_shouldReturnBadRequest_whenProjectNameBlank() throws Exception {
        ProjectExecutorsRequest request = new ProjectExecutorsRequest(" ");

        mockMvc.perform(post("/api/projects/executors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(projectDirectoryFacade, never()).getProjectExecutors(anyString());
    }

    @Test
    void getProjectExecutors_shouldReturnBadRequest_whenProjectNameTooLong() throws Exception {
        ProjectExecutorsRequest request = new ProjectExecutorsRequest("a".repeat(121));

        mockMvc.perform(post("/api/projects/executors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(projectDirectoryFacade, never()).getProjectExecutors(anyString());
    }
}
