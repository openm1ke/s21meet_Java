package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectsProviderTest {

    @Test
    void refreshStudentProjectsByLogin_shouldDelegateToGetStudentProjectsByLoginByDefault() {
        AtomicInteger calls = new AtomicInteger();
        ProjectsProvider provider = login -> {
            calls.incrementAndGet();
            return List.of();
        };

        provider.refreshStudentProjectsByLogin("login");

        assertEquals(1, calls.get());
    }
}
