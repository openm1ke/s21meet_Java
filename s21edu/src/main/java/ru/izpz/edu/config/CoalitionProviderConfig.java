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
import ru.izpz.edu.service.provider.CoalitionProvider;
import ru.izpz.edu.service.provider.GraphQLCoalitionProvider;
import ru.izpz.edu.service.provider.RestApiCoalitionProvider;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class CoalitionProviderConfig {

    @Bean
    @Primary
    public CoalitionProvider coalitionProvider(
            ObjectProvider<RestApiCoalitionProvider> restProvider,
            ObjectProvider<GraphQLCoalitionProvider> graphQlProvider,
            CoalitionProperties properties) {

        CoalitionProvider rest = restProvider.getIfAvailable();
        CoalitionProvider graphQl = graphQlProvider.getIfAvailable();
        String providerType = properties.getProvider() == null ? "auto" : properties.getProvider().toLowerCase();

        return switch (providerType) {
            case "graphql" -> requireProvider(graphQl, "graphql");
            case "rest" -> requireProvider(rest, "rest");
            case "auto" -> {
                if (graphQl != null) {
                    log.info("Using GraphQL coalition provider (auto)");
                    yield graphQl;
                }
                if (rest != null) {
                    log.info("Using REST coalition provider (auto fallback)");
                    yield rest;
                }
                throw new IllegalStateException("No coalition provider bean is available");
            }
            default -> throw new IllegalArgumentException("Unknown coalition provider: " + providerType);
        };
    }

    private CoalitionProvider requireProvider(CoalitionProvider provider, String name) {
        if (provider == null) {
            throw new IllegalStateException("Configured coalition provider is not available: " + name);
        }
        log.info("Using {} coalition provider", name.toUpperCase());
        return provider;
    }

    @Bean
    @ConfigurationProperties(prefix = "coalition")
    public CoalitionProperties coalitionProperties() {
        return new CoalitionProperties();
    }

    @Setter
    @Getter
    public static class CoalitionProperties {
        private String provider = "auto";
        private Rest rest = new Rest();

        @Setter
        @Getter
        public static class Rest {
            private boolean fetchMemberCount = false;
            private int pageSize = 1000;
        }
    }
}
