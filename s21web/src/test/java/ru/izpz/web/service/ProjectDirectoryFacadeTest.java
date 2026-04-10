package ru.izpz.web.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.client.EduProfileClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDirectoryFacadeTest {

    @Mock
    private EduProfileClient eduProfileClient;

    @Test
    void getProjectNames_shouldDelegateToEduClient() {
        ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
        when(eduProfileClient.getProjectNames()).thenReturn(List.of("A1_Maze_C"));

        List<String> result = facade.getProjectNames();

        assertEquals(List.of("A1_Maze_C"), result);
        verify(eduProfileClient).getProjectNames();
    }

    @Test
    void getProjectExecutors_shouldDelegateWithRequest() {
        ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
        when(eduProfileClient.getProjectExecutors(any(ProjectExecutorsRequest.class)))
            .thenReturn(List.of(new ProjectExecutorDto("mike", "MSK", "IN_PROGRESS", null)));

        List<ProjectExecutorDto> result = facade.getProjectExecutors("A1_Maze_C");

        assertEquals(1, result.size());
        assertEquals("mike", result.get(0).login());
        verify(eduProfileClient).getProjectExecutors(new ProjectExecutorsRequest("A1_Maze_C"));
    }
}

