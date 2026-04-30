package ru.izpz.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.izpz.auth.config.RestTemplateConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.main.lazy-initialization=true")
@ActiveProfiles("test")
class ApplicationContextIntegrationTest {

  @Autowired private RestTemplateConfig restTemplateConfig;

  @Test
  void contextLoads() {
    assertNotNull(restTemplateConfig);
  }
}
