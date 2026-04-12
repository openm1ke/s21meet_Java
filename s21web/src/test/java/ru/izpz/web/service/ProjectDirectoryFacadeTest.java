package ru.izpz.web.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.CampusRequest;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.client.EduProfileClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDirectoryFacadeTest {

    @Mock
    private EduProfileClient eduProfileClient;

    @Test
    void getProjectNames_shouldDelegateToEduClient() {
        ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
        when(eduProfileClient.getProjectNames(any(CampusRequest.class))).thenReturn(List.of("A1_Maze_C"));

        List<String> result = facade.getProjectNames("123456");

        assertEquals(List.of("A1_Maze_C"), result);
        verify(eduProfileClient).getProjectNames(argThat(req -> req != null && "123456".equals(req.getTelegramId())));
    }

    @Test
    void getProjectExecutors_shouldDelegateWithRequest() {
        ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
        when(eduProfileClient.getProjectExecutors(any(ProjectExecutorsRequest.class)))
            .thenReturn(List.of(new ProjectExecutorDto("mike", "MSK", "IN_PROGRESS", null)));

        ProjectExecutorsRequest request = new ProjectExecutorsRequest("A1_Maze_C");
        List<ProjectExecutorDto> result = facade.getProjectExecutors(request);

        assertEquals(1, result.size());
        assertEquals("mike", result.get(0).login());
        verify(eduProfileClient).getProjectExecutors(new ProjectExecutorsRequest("A1_Maze_C"));
    }

    @Test
    void getAllProjectNames_shouldDelegateToEduClient() {
        ProjectDirectoryFacade facade = new ProjectDirectoryFacade(eduProfileClient);
        when(eduProfileClient.getAllProjectNames()).thenReturn(List.of("A1_Maze_C", "C2_SimpleBashUtils"));

        List<String> result = facade.getAllProjectNames();

        assertEquals(List.of("A1_Maze_C", "C2_SimpleBashUtils"), result);
        verify(eduProfileClient).getAllProjectNames();
    }
}
