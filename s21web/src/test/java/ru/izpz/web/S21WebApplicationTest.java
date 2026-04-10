package ru.izpz.web;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class S21WebApplicationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void main_shouldDelegateToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            S21WebApplication.main(new String[]{"--spring.main.web-application-type=none"});
            springApplication.verify(() -> SpringApplication.run(S21WebApplication.class, new String[]{"--spring.main.web-application-type=none"}));
        }
    }
}
