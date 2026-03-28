package ru.izpz.edu.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import ru.izpz.edu.service.provider.CoalitionProvider;
import ru.izpz.edu.service.provider.GraphQLCoalitionProvider;
import ru.izpz.edu.service.provider.RestApiCoalitionProvider;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoalitionProviderConfigTest {

    private final CoalitionProviderConfig config = new CoalitionProviderConfig();

    @Test
    void coalitionProvider_shouldReturnGraphQl_whenConfigured() {
        RestApiCoalitionProvider rest = mock(RestApiCoalitionProvider.class);
        GraphQLCoalitionProvider graph = mock(GraphQLCoalitionProvider.class);
        ObjectProvider<RestApiCoalitionProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLCoalitionProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(rest);
        when(graphProvider.getIfAvailable()).thenReturn(graph);

        CoalitionProviderConfig.CoalitionProperties properties = new CoalitionProviderConfig.CoalitionProperties();
        properties.setProvider("graphql");

        CoalitionProvider result = config.coalitionProvider(restProvider, graphProvider, properties);

        assertSame(graph, result);
    }

    @Test
    void coalitionProvider_shouldReturnRest_whenConfigured() {
        RestApiCoalitionProvider rest = mock(RestApiCoalitionProvider.class);
        GraphQLCoalitionProvider graph = mock(GraphQLCoalitionProvider.class);
        ObjectProvider<RestApiCoalitionProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLCoalitionProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(rest);
        when(graphProvider.getIfAvailable()).thenReturn(graph);

        CoalitionProviderConfig.CoalitionProperties properties = new CoalitionProviderConfig.CoalitionProperties();
        properties.setProvider("rest");

        CoalitionProvider result = config.coalitionProvider(restProvider, graphProvider, properties);

        assertSame(rest, result);
    }

    @Test
    void coalitionProvider_shouldPreferGraphQl_whenAutoAndBothAvailable() {
        RestApiCoalitionProvider rest = mock(RestApiCoalitionProvider.class);
        GraphQLCoalitionProvider graph = mock(GraphQLCoalitionProvider.class);
        ObjectProvider<RestApiCoalitionProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLCoalitionProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(rest);
        when(graphProvider.getIfAvailable()).thenReturn(graph);

        CoalitionProviderConfig.CoalitionProperties properties = new CoalitionProviderConfig.CoalitionProperties();
        properties.setProvider("auto");

        CoalitionProvider result = config.coalitionProvider(restProvider, graphProvider, properties);

        assertSame(graph, result);
    }

    @Test
    void coalitionProvider_shouldFallbackToRest_whenAutoAndGraphQlMissing() {
        RestApiCoalitionProvider rest = mock(RestApiCoalitionProvider.class);
        ObjectProvider<RestApiCoalitionProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLCoalitionProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(rest);
        when(graphProvider.getIfAvailable()).thenReturn(null);

        CoalitionProviderConfig.CoalitionProperties properties = new CoalitionProviderConfig.CoalitionProperties();
        properties.setProvider("auto");

        CoalitionProvider result = config.coalitionProvider(restProvider, graphProvider, properties);

        assertSame(rest, result);
    }

    @Test
    void coalitionProvider_shouldThrow_whenConfiguredProviderMissing() {
        ObjectProvider<RestApiCoalitionProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLCoalitionProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(null);
        when(graphProvider.getIfAvailable()).thenReturn(null);

        CoalitionProviderConfig.CoalitionProperties properties = new CoalitionProviderConfig.CoalitionProperties();
        properties.setProvider("graphql");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.coalitionProvider(restProvider, graphProvider, properties)
        );
        assertEquals("Configured coalition provider is not available: graphql", exception.getMessage());
    }

    @Test
    void coalitionProvider_shouldThrow_whenAutoAndNoProviderAvailable() {
        ObjectProvider<RestApiCoalitionProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLCoalitionProvider> graphProvider = mock(ObjectProvider.class);
        when(restProvider.getIfAvailable()).thenReturn(null);
        when(graphProvider.getIfAvailable()).thenReturn(null);

        CoalitionProviderConfig.CoalitionProperties properties = new CoalitionProviderConfig.CoalitionProperties();
        properties.setProvider("auto");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.coalitionProvider(restProvider, graphProvider, properties)
        );
        assertEquals("No coalition provider bean is available", exception.getMessage());
    }

    @Test
    void coalitionProvider_shouldThrow_whenProviderIsUnknown() {
        ObjectProvider<RestApiCoalitionProvider> restProvider = mock(ObjectProvider.class);
        ObjectProvider<GraphQLCoalitionProvider> graphProvider = mock(ObjectProvider.class);

        CoalitionProviderConfig.CoalitionProperties properties = new CoalitionProviderConfig.CoalitionProperties();
        properties.setProvider("custom");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> config.coalitionProvider(restProvider, graphProvider, properties)
        );
        assertEquals("Unknown coalition provider: custom", exception.getMessage());
    }

    @Test
    void coalitionProperties_shouldCreateDefaultBean() {
        CoalitionProviderConfig.CoalitionProperties properties = config.coalitionProperties();

        assertNotNull(properties);
        assertEquals("auto", properties.getProvider());
        assertEquals(Duration.ofMinutes(15), properties.getRefreshTtl());
        assertNotNull(properties.getRest());
        assertEquals(1000, properties.getRest().getPageSize());
    }
}
