package ru.izpz.edu.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import ru.izpz.dto.NotifyRequest;

class BotClientTest {

  @Test
  void contract_shouldExposeExpectedFeignAndEndpointMetadata() throws Exception {
    FeignClient feignClient = BotClient.class.getAnnotation(FeignClient.class);
    assertNotNull(feignClient);
    assertEquals("botclient", feignClient.name());
    assertEquals("/api", feignClient.path());

    Method notify = BotClient.class.getMethod("notify", NotifyRequest.class);
    PostMapping postMapping = notify.getAnnotation(PostMapping.class);
    assertNotNull(postMapping);
    assertTrue(postMapping.value().length > 0);
    assertEquals("/notify", postMapping.value()[0]);
  }
}
