package ru.izpz.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FeignTimeoutPropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withPropertyValues(
              "spring.cloud.openfeign.client.config.default.connectTimeout=3456",
              "spring.cloud.openfeign.client.config.default.readTimeout=23456",
              "telegram.webapp.auth.enabled=false");

  @Test
  void shouldLoadFeignTimeoutProperties() {
    contextRunner.run(
        context -> {
          assertEquals(
              "3456",
              context
                  .getEnvironment()
                  .getProperty("spring.cloud.openfeign.client.config.default.connectTimeout"));
          assertEquals(
              "23456",
              context
                  .getEnvironment()
                  .getProperty("spring.cloud.openfeign.client.config.default.readTimeout"));
        });
  }
}
