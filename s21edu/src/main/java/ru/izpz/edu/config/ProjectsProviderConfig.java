package ru.izpz.edu.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.izpz.edu.service.provider.GraphQLProjectsProvider;
import ru.izpz.edu.service.provider.ProjectsProvider;
import ru.izpz.edu.service.provider.RestApiProjectsProvider;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class ProjectsProviderConfig {

    @Bean
    @Primary
    public ProjectsProvider projectsProvider(
            ObjectProvider<RestApiProjectsProvider> restProvider,
            ObjectProvider<GraphQLProjectsProvider> graphQlProvider,
            ProjectsProperties properties
    ) {
        ProjectsProvider rest = restProvider.getIfAvailable();
        ProjectsProvider graphQl = graphQlProvider.getIfAvailable();
        String providerType = properties.getProvider() == null ? "auto" : properties.getProvider().toLowerCase();

        return switch (providerType) {
            case "graphql" -> requireProvider(graphQl, "graphql");
            case "rest" -> requireProvider(rest, "rest");
            case "auto" -> {
                if (graphQl != null) {
                    log.info("Using GraphQL projects provider (auto)");
                    yield graphQl;
                }
                if (rest != null) {
                    log.info("Using REST projects provider (auto fallback)");
                    yield rest;
                }
                throw new IllegalStateException("No projects provider bean is available");
            }
            default -> throw new IllegalArgumentException("Unknown projects provider: " + providerType);
        };
    }

    private ProjectsProvider requireProvider(ProjectsProvider provider, String name) {
        if (provider == null) {
            throw new IllegalStateException("Configured projects provider is not available: " + name);
        }
        log.info("Using {} projects provider", name.toUpperCase());
        return provider;
    }

    @Bean
    @ConfigurationProperties(prefix = "projects")
    public ProjectsProperties projectsProperties() {
        return new ProjectsProperties();
    }

    @Setter
    @Getter
    public static class ProjectsProperties {
        private String provider = "auto";
        private Rest rest = new Rest();

        @Setter
        @Getter
        public static class Rest {
            private int pageSize = 1000;
        }
    }
}
