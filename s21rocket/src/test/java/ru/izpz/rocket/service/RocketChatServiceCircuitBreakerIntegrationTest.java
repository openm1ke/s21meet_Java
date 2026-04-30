package ru.izpz.rocket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.izpz.dto.RocketChatSendResponse;

@SpringBootTest(
    properties = {
      "ROCKET_CHAT_URL=ws://localhost:3000/websocket",
      "ROCKET_CHAT_QR_BOT=test-bot",
      "ROCKET_CHAT_TOKEN=test-token",
      "ROCKETCHAT_RETRY_MAX_ATTEMPTS=1"
    })
class RocketChatServiceCircuitBreakerIntegrationTest {

  private static final String OPERATION = "rocketchatOperation";
  private static final String SERVICE_UNAVAILABLE_MESSAGE =
      "Сервис Rocket.Chat временно недоступен, попробуйте позже";

  @Autowired private RocketChatService rocketChatService;
  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  private CircuitBreaker circuitBreaker;

  @BeforeEach
  void setUp() {
    circuitBreaker = circuitBreakerRegistry.circuitBreaker(OPERATION);
    circuitBreaker.transitionToOpenState();
  }

  @AfterEach
  void tearDown() {
    circuitBreaker.reset();
  }

  @Test
  void generateQrCodeResilient_shouldUseFallback_whenCircuitBreakerIsOpen() {
    RocketChatSendResponse result = rocketChatService.generateQrCodeResilient();

    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals(SERVICE_UNAVAILABLE_MESSAGE, result.getMessage());
  }

  @Test
  void sendVerificationCodeResilient_shouldUseFallback_whenCircuitBreakerIsOpen() {
    RocketChatSendResponse result =
        rocketChatService.sendVerificationCodeResilient("target-user", "verification-code");

    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals(SERVICE_UNAVAILABLE_MESSAGE, result.getMessage());
  }
}
