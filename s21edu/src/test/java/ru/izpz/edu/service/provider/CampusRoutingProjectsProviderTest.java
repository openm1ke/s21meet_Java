package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.model.StudentCredentials;
import ru.izpz.edu.repository.StudentCredentialsRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusRoutingProjectsProviderTest {

    @Mock
    private ObjectProvider<GraphQLProjectsProvider> graphQlProvider;
    @Mock
    private ObjectProvider<RestApiProjectsProvider> restProvider;
    @Mock
    private StudentCredentialsRepository studentCredentialsRepository;
    @Mock
    private GraphQLProjectsProvider graphQLProjectsProvider;
    @Mock
    private RestApiProjectsProvider restApiProjectsProvider;

    private CampusRoutingProjectsProvider routingProvider;

    @BeforeEach
    void setUp() {
        routingProvider = new CampusRoutingProjectsProvider(
            graphQlProvider,
            restProvider,
            studentCredentialsRepository
        );
        ReflectionTestUtils.setField(routingProvider, "graphQlSchoolId", "MSK");
    }

    @Test
    void getStudentProjectsByLogin_shouldUseGraphQlForMsk() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setSchoolId("MSK");
        when(studentCredentialsRepository.findById("login")).thenReturn(Optional.of(credentials));
        when(graphQlProvider.getIfAvailable()).thenReturn(graphQLProjectsProvider);
        when(restProvider.getIfAvailable()).thenReturn(restApiProjectsProvider);

        StudentProjectData project = new StudentProjectData("1", "name", null, null, null, null, null, null, null, null, null);
        when(graphQLProjectsProvider.getStudentProjectsByLogin("login")).thenReturn(List.of(project));

        List<StudentProjectData> result = routingProvider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        verify(graphQLProjectsProvider).getStudentProjectsByLogin("login");
        verify(restApiProjectsProvider, never()).getStudentProjectsByLogin("login");
    }

    @Test
    void getStudentProjectsByLogin_shouldUseRestForNonMsk() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setSchoolId("KZN");
        when(studentCredentialsRepository.findById("login")).thenReturn(Optional.of(credentials));
        when(graphQlProvider.getIfAvailable()).thenReturn(graphQLProjectsProvider);
        when(restProvider.getIfAvailable()).thenReturn(restApiProjectsProvider);
        when(restApiProjectsProvider.getStudentProjectsByLogin("login")).thenReturn(List.of());

        List<StudentProjectData> result = routingProvider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
        verify(restApiProjectsProvider).getStudentProjectsByLogin("login");
        verify(graphQLProjectsProvider, never()).getStudentProjectsByLogin("login");
    }

    @Test
    void refreshStudentProjects_shouldFallbackToRestWhenGraphQlUnavailableForMsk() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setLogin("login");
        credentials.setSchoolId("MSK");
        when(graphQlProvider.getIfAvailable()).thenReturn(null);
        when(restProvider.getIfAvailable()).thenReturn(restApiProjectsProvider);

        CampusRoutingProjectsProvider.RefreshResult result = routingProvider.refreshStudentProjects(credentials);

        assertEquals(CampusRoutingProjectsProvider.RefreshResult.SUCCESS, result);
        verify(restApiProjectsProvider).refreshStudentProjectsByLogin("login");
    }

    @Test
    void refreshStudentProjects_shouldSkipWhenNoProviderAvailable() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setLogin("login");
        credentials.setSchoolId("MSK");
        when(graphQlProvider.getIfAvailable()).thenReturn(null);
        when(restProvider.getIfAvailable()).thenReturn(null);

        CampusRoutingProjectsProvider.RefreshResult result = routingProvider.refreshStudentProjects(credentials);

        assertEquals(CampusRoutingProjectsProvider.RefreshResult.SKIPPED_NO_PROVIDER, result);
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnEmptyWhenNoProvidersAvailable() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setSchoolId("KZN");
        when(studentCredentialsRepository.findById("login")).thenReturn(Optional.of(credentials));
        when(graphQlProvider.getIfAvailable()).thenReturn(null);
        when(restProvider.getIfAvailable()).thenReturn(null);

        List<StudentProjectData> result = routingProvider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnEmptyWhenMskAndNoProvidersAvailable() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setSchoolId("MSK");
        when(studentCredentialsRepository.findById("login")).thenReturn(Optional.of(credentials));
        when(graphQlProvider.getIfAvailable()).thenReturn(null);
        when(restProvider.getIfAvailable()).thenReturn(null);

        List<StudentProjectData> result = routingProvider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
    }

    @Test
    void refreshStudentProjectsByLogin_shouldUseRepositorySchoolIdAndFallbackToGraphQlForNonMsk() {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setSchoolId("KZN");
        when(studentCredentialsRepository.findById("login")).thenReturn(Optional.of(credentials));
        when(restProvider.getIfAvailable()).thenReturn(null);
        when(graphQlProvider.getIfAvailable()).thenReturn(graphQLProjectsProvider);

        CampusRoutingProjectsProvider.RefreshResult result = routingProvider.refreshStudentProjects("login");

        assertEquals(CampusRoutingProjectsProvider.RefreshResult.SUCCESS, result);
        verify(graphQLProjectsProvider).refreshStudentProjectsByLogin("login");
        verify(restApiProjectsProvider, never()).refreshStudentProjectsByLogin("login");
    }
}
