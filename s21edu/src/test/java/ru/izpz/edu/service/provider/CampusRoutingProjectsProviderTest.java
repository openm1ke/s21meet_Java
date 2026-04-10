package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.model.StudentCredentials;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusRoutingProjectsProviderTest {

    @Mock
    private ObjectProvider<RestApiProjectsProvider> restProvider;
    @Mock
    private RestApiProjectsProvider restApiProjectsProvider;

    private CampusRoutingProjectsProvider routingProvider;

    @BeforeEach
    void setUp() {
        routingProvider = new CampusRoutingProjectsProvider(restProvider);
    }

    @Test
    void getStudentProjectsByLogin_shouldUseRestProvider() {
        when(restProvider.getIfAvailable()).thenReturn(restApiProjectsProvider);
        StudentProjectData project = new StudentProjectData("1", "name", null, null, null, null, null, null, null, null, null);
        when(restApiProjectsProvider.getStudentProjectsByLogin("login")).thenReturn(List.of(project));

        List<StudentProjectData> result = routingProvider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        verify(restApiProjectsProvider).getStudentProjectsByLogin("login");
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnEmptyWhenNoProviderAvailable() {
        when(restProvider.getIfAvailable()).thenReturn(null);

        List<StudentProjectData> result = routingProvider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
    }

    @Test
    void refreshStudentProjects_shouldCallRestProviderWhenAvailable() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setLogin("login");
        when(restProvider.getIfAvailable()).thenReturn(restApiProjectsProvider);

        CampusRoutingProjectsProvider.RefreshResult result = routingProvider.refreshStudentProjects(credentials);

        assertEquals(CampusRoutingProjectsProvider.RefreshResult.SUCCESS, result);
        verify(restApiProjectsProvider).refreshStudentProjectsByLogin("login");
    }

    @Test
    void refreshStudentProjects_shouldSkipWhenNoProviderAvailable() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setLogin("login");
        when(restProvider.getIfAvailable()).thenReturn(null);

        CampusRoutingProjectsProvider.RefreshResult result = routingProvider.refreshStudentProjects(credentials);

        assertEquals(CampusRoutingProjectsProvider.RefreshResult.SKIPPED_NO_PROVIDER, result);
        verify(restApiProjectsProvider, never()).refreshStudentProjectsByLogin("login");
    }

    @Test
    void providerTypeForSchoolId_shouldReturnRestWhenProviderAvailable() {
        when(restProvider.getIfAvailable()).thenReturn(restApiProjectsProvider);

        String result = routingProvider.providerTypeForSchoolId();

        assertEquals("rest", result);
    }
}
