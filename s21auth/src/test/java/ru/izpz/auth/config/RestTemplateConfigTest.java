package ru.izpz.auth.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class RestTemplateConfigTest {

  @Test
  void restTemplate_shouldBuildWithConfiguredTimeouts() {
    RestTemplateConfig config = new RestTemplateConfig();
    ReflectionTestUtils.setField(config, "connectTimeout", Duration.ofSeconds(3));
    ReflectionTestUtils.setField(config, "readTimeout", Duration.ofSeconds(17));

    RestTemplate restTemplate = config.restTemplate(new RestTemplateBuilder());

    assertNotNull(restTemplate);
  }
}
