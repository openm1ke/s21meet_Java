package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.service.GraphQLService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphQLProjectsProviderTest {

    @Mock
    private GraphQLService graphQLService;

    @InjectMocks
    private GraphQLProjectsProvider provider;

    @Test
    void getStudentProjectsByLogin_shouldDelegateToGraphqlCache() {
        StudentProjectData project = new StudentProjectData(
                "g", "n", null, null, null, null, null, null, "IN_PROGRESS", null, null
        );
        when(graphQLService.getCachedStudentProjectsByLogin("login")).thenReturn(List.of(project));

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        verify(graphQLService).getCachedStudentProjectsByLogin("login");
    }
}
