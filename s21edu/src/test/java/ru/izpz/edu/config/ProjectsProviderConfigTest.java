package ru.izpz.edu.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import ru.izpz.edu.service.provider.GraphQLProjectsProvider;
import ru.izpz.edu.service.provider.ProjectsProvider;
import ru.izpz.edu.service.provider.RestApiProjectsProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectsProviderConfigTest {

    private final ProjectsProviderConfig config = new ProjectsProviderConfig();

    @Test
    void projectsProvider_shouldReturnGraphQl_whenConfigured() {
        RestApiProjectsProvider rest = mock(RestApiProjectsProvider.class);
        GraphQLProjectsProvider graph = mock(GraphQLProjectsProvider.class);
        ObjectProvider<RestApiProjectsProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLProjectsProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(rest);
        when(graphProvider.getIfAvailable()).thenReturn(graph);

        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.setProvider("graphql");

        ProjectsProvider result = config.projectsProvider(restProvider, graphProvider, properties);

        assertSame(graph, result);
    }

    @Test
    void projectsProvider_shouldReturnRest_whenConfigured() {
        RestApiProjectsProvider rest = mock(RestApiProjectsProvider.class);
        GraphQLProjectsProvider graph = mock(GraphQLProjectsProvider.class);
        ObjectProvider<RestApiProjectsProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLProjectsProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(rest);
        when(graphProvider.getIfAvailable()).thenReturn(graph);

        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.setProvider("rest");

        ProjectsProvider result = config.projectsProvider(restProvider, graphProvider, properties);

        assertSame(rest, result);
    }

    @Test
    void projectsProperties_shouldCreatePropertiesBean() {
        ProjectsProviderConfig.ProjectsProperties properties = config.projectsProperties();
        assertNotNull(properties);
    }
}
