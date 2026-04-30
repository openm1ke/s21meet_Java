package ru.izpz.bot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.main.lazy-initialization=true")
@ActiveProfiles("test")
class ApplicationContextIntegrationTest {

  @Test
  void contextLoads() {
    // Если контекст не загрузится, тест упадёт.
  }
}
