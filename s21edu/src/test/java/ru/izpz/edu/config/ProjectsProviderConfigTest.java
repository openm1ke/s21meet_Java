package ru.izpz.edu.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import ru.izpz.edu.service.provider.GraphQLProjectsProvider;
import ru.izpz.edu.service.provider.ProjectsProvider;
import ru.izpz.edu.service.provider.RestApiProjectsProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals("auto", properties.getProvider());
        assertNotNull(properties.getRest());
        assertEquals(1000, properties.getRest().getPageSize());
    }

    @Test
    void projectsProvider_shouldPreferGraphQl_whenAutoAndBothAvailable() {
        RestApiProjectsProvider rest = mock(RestApiProjectsProvider.class);
        GraphQLProjectsProvider graph = mock(GraphQLProjectsProvider.class);
        ObjectProvider<RestApiProjectsProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLProjectsProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(rest);
        when(graphProvider.getIfAvailable()).thenReturn(graph);

        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.setProvider("auto");

        ProjectsProvider result = config.projectsProvider(restProvider, graphProvider, properties);

        assertSame(graph, result);
    }

    @Test
    void projectsProvider_shouldFallbackToRest_whenAutoAndGraphQlMissing() {
        RestApiProjectsProvider rest = mock(RestApiProjectsProvider.class);
        ObjectProvider<RestApiProjectsProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLProjectsProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(rest);
        when(graphProvider.getIfAvailable()).thenReturn(null);

        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.setProvider("auto");

        ProjectsProvider result = config.projectsProvider(restProvider, graphProvider, properties);

        assertSame(rest, result);
    }

    @Test
    void projectsProvider_shouldThrow_whenConfiguredProviderMissing() {
        ObjectProvider<RestApiProjectsProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLProjectsProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(null);
        when(graphProvider.getIfAvailable()).thenReturn(null);

        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.setProvider("graphql");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.projectsProvider(restProvider, graphProvider, properties)
        );
        assertEquals("Configured projects provider is not available: graphql", exception.getMessage());
    }

    @Test
    void projectsProvider_shouldThrow_whenAutoAndNoProviderAvailable() {
        ObjectProvider<RestApiProjectsProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLProjectsProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(null);
        when(graphProvider.getIfAvailable()).thenReturn(null);

        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.setProvider("auto");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.projectsProvider(restProvider, graphProvider, properties)
        );
        assertEquals("No projects provider bean is available", exception.getMessage());
    }

    @Test
    void projectsProvider_shouldThrow_whenProviderIsUnknown() {
        ObjectProvider<RestApiProjectsProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLProjectsProvider> graphProvider = mock(ObjectProvider.class);

        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.setProvider("custom");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> config.projectsProvider(restProvider, graphProvider, properties)
        );
        assertEquals("Unknown projects provider: custom", exception.getMessage());
    }
}
