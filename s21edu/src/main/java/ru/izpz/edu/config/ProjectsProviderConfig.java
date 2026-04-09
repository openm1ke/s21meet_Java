package ru.izpz.edu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class ProjectsProviderConfig {

    @Bean
    @ConfigurationProperties(prefix = "projects")
    public ProjectsProperties projectsProperties() {
        return new ProjectsProperties();
    }

    @Setter
    @Getter
    public static class ProjectsProperties {
        private Duration refreshTtl = Duration.ofMinutes(15);
        private Rest rest = new Rest();

        @Setter
        @Getter
        public static class Rest {
            private int pageSize = 1000;
        }
    }
}
