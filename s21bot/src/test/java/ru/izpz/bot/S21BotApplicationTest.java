package ru.izpz.bot;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class S21BotApplicationTest {

    @Test
    void main_runsSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            S21BotApplication.main(new String[0]);
            spring.verify(() -> SpringApplication.run(S21BotApplication.class, new String[0]));
        }
    }
}
