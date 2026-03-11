package ru.izpz.edu.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.izpz.edu.service.provider.GraphQLWorkplaceProvider;
import ru.izpz.edu.service.provider.RestApiWorkplaceProvider;
import ru.izpz.edu.service.provider.WorkplaceProvider;

/**
 * Configuration for WorkplaceProvider selection based on application.yml
 */
@Slf4j
@Configuration
public class WorkplaceProviderConfig {

    @Bean
    @Primary
    public WorkplaceProvider workplaceProvider(
            RestApiWorkplaceProvider restApiWorkplaceProvider,
            GraphQLWorkplaceProvider graphQLWorkplaceProvider,
            WorkplaceProperties properties) {
        
        String providerType = properties.getProvider();
        log.info("Initializing WorkplaceProvider with type: {}", providerType);

        return switch (providerType.toLowerCase()) {
            case "graphql" -> {
                log.info("Using GraphQL WorkplaceProvider");
                yield graphQLWorkplaceProvider;
            }
            default -> {
                log.info("Using REST API WorkplaceProvider (default)");
                yield restApiWorkplaceProvider;
            }
        };
    }

    @Bean
    @ConfigurationProperties(prefix = "campus.workplace")
    public WorkplaceProperties workplaceProperties() {
        return new WorkplaceProperties();
    }

    @Setter
    @Getter
    public static class WorkplaceProperties {
        String provider;
    }
}
