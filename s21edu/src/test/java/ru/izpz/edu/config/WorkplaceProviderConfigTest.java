package ru.izpz.edu.config;

import org.junit.jupiter.api.Test;
import ru.izpz.edu.service.provider.GraphQLWorkplaceProvider;
import ru.izpz.edu.service.provider.RestApiWorkplaceProvider;
import ru.izpz.edu.service.provider.WorkplaceProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class WorkplaceProviderConfigTest {

    private final WorkplaceProviderConfig config = new WorkplaceProviderConfig();

    @Test
    void workplaceProvider_shouldReturnGraphQlProvider_whenConfigured() {
        RestApiWorkplaceProvider rest = mock(RestApiWorkplaceProvider.class);
        GraphQLWorkplaceProvider graph = mock(GraphQLWorkplaceProvider.class);
        WorkplaceProviderConfig.WorkplaceProperties properties = new WorkplaceProviderConfig.WorkplaceProperties();
        properties.setProvider("graphql");

        WorkplaceProvider result = config.workplaceProvider(rest, graph, properties);

        assertSame(graph, result);
    }

    @Test
    void workplaceProvider_shouldReturnRestProvider_forDefaultBranch() {
        RestApiWorkplaceProvider rest = mock(RestApiWorkplaceProvider.class);
        GraphQLWorkplaceProvider graph = mock(GraphQLWorkplaceProvider.class);
        WorkplaceProviderConfig.WorkplaceProperties properties = new WorkplaceProviderConfig.WorkplaceProperties();
        properties.setProvider("rest");

        WorkplaceProvider result = config.workplaceProvider(rest, graph, properties);

        assertSame(rest, result);
    }

    @Test
    void workplaceProperties_shouldCreatePropertiesBean() {
        WorkplaceProviderConfig.WorkplaceProperties properties = config.workplaceProperties();
        assertNotNull(properties);
    }
}
