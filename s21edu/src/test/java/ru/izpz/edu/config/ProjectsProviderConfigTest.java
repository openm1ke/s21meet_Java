package ru.izpz.edu.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProjectsProviderConfigTest {

    private final ProjectsProviderConfig config = new ProjectsProviderConfig();

    @Test
    void projectsProperties_shouldCreatePropertiesBean() {
        ProjectsProviderConfig.ProjectsProperties properties = config.projectsProperties();
        assertNotNull(properties);
        assertEquals(Duration.ofMinutes(15), properties.getRefreshTtl());
        assertNotNull(properties.getRest());
        assertEquals(1000, properties.getRest().getPageSize());
    }
}
