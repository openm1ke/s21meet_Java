package ru.izpz.edu;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class S21EduApplicationTest {

  @Test
  void main_runsSpringApplication() {
    try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
      S21EduApplication.main(new String[0]);
      spring.verify(() -> SpringApplication.run(S21EduApplication.class, new String[0]));
    }
  }
}
