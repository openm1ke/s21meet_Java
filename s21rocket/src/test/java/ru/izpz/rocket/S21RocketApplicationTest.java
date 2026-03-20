package ru.izpz.rocket;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class S21RocketApplicationTest {

    @Test
    void constructor_isCallable() {
        new S21RocketApplication();
    }

    @Test
    void main_runsSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            S21RocketApplication.main(new String[0]);
            spring.verify(() -> SpringApplication.run(S21RocketApplication.class, new String[0]));
        }
    }
}
