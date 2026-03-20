package ru.izpz.rocket;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class S21RocketApplicationTest {

    @Test
    void constructor_isCallable() {
        S21RocketApplication app = new S21RocketApplication();
        assertNotNull(app);
    }

    @Test
    void main_runsSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            S21RocketApplication.main(new String[0]);
            spring.verify(() -> SpringApplication.run(S21RocketApplication.class, new String[0]));
        }
    }
}
