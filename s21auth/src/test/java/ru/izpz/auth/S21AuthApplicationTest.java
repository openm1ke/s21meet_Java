package ru.izpz.auth;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class S21AuthApplicationTest {

  @Test
  void main_runsSpringApplication() {
    try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
      S21AuthApplication.main(new String[0]);
      spring.verify(() -> SpringApplication.run(S21AuthApplication.class, new String[0]));
    }
  }
}
