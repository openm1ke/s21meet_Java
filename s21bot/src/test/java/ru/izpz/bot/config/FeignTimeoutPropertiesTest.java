package ru.izpz.bot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FeignTimeoutPropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withPropertyValues(
              "spring.cloud.openfeign.client.config.default.connectTimeout=4567",
              "spring.cloud.openfeign.client.config.default.readTimeout=34567");

  @Test
  void shouldLoadFeignTimeoutProperties() {
    contextRunner.run(
        context -> {
          assertEquals(
              "4567",
              context
                  .getEnvironment()
                  .getProperty("spring.cloud.openfeign.client.config.default.connectTimeout"));
          assertEquals(
              "34567",
              context
                  .getEnvironment()
                  .getProperty("spring.cloud.openfeign.client.config.default.readTimeout"));
        });
  }
}
