package ru.izpz.edu.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.izpz.edu.S21EduApplication;
import ru.izpz.edu.exception.PlatformRateLimitException;
import ru.izpz.edu.exception.PlatformTransientException;
import ru.izpz.edu.exception.PlatformTransportException;
import ru.izpz.edu.exception.PlatformUnauthorizedException;

/** Проверяет фактический отбор ошибок Resilience4j перед обращением к внешней платформе. */
@SpringBootTest(
    classes = {S21EduApplication.class, RetryPolicyIntegrationTest.RetryPolicyConfiguration.class},
    properties = {
      "spring.task.scheduling.enabled=false",
      "projects.scheduler.enabled=false",
      "event.scheduler.enabled=false",
      "resilience4j.retry.instances.externalGlobal.maxAttempts=2",
      "resilience4j.retry.instances.externalGlobal.waitDuration=1ms",
      "resilience4j.retry.instances.participantProfile.maxAttempts=3",
      "resilience4j.retry.instances.participantProfile.waitDuration=1ms"
    })
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class RetryPolicyIntegrationTest {

  @MockitoBean private ru.izpz.edu.service.GraphQLService graphQlService;

  @Autowired private RetryProbe retryProbe;

  @BeforeEach
  void resetAttempts() {
    retryProbe.reset();
  }

  @Test
  void externalGlobal_shouldNotRetryUnauthorizedErrors() {
    PlatformUnauthorizedException exception =
        new PlatformUnauthorizedException("forbidden", 403, Collections.emptyMap(), null);

    assertThrows(PlatformUnauthorizedException.class, () -> retryProbe.externalCall(exception));

    assertEquals(1, retryProbe.getInvocationCount());
  }

  @Test
  void externalGlobal_shouldRetryRateLimitAndTransientErrors() {
    PlatformRateLimitException rateLimitException =
        new PlatformRateLimitException("rate limited", 429, Collections.emptyMap(), null);
    final PlatformTransientException transientException =
        new PlatformTransientException("unavailable", 503, Collections.emptyMap(), null);

    assertThrows(
        PlatformRateLimitException.class, () -> retryProbe.externalCall(rateLimitException));
    assertEquals(2, retryProbe.getInvocationCount());

    retryProbe.reset();
    assertThrows(
        PlatformTransientException.class, () -> retryProbe.externalCall(transientException));
    assertEquals(2, retryProbe.getInvocationCount());
  }

  @Test
  void participantProfile_shouldRetryTransportErrorsWithItsOwnLimit() {
    PlatformTransportException exception =
        new PlatformTransportException("connection failed", new IOException("timeout"));

    assertThrows(
        PlatformTransportException.class, () -> retryProbe.participantProfileCall(exception));

    assertEquals(3, retryProbe.getInvocationCount());
  }

  @Test
  void graphqlGlobal_shouldNotRetryUnauthorizedErrors() {
    GraphQlUnauthorizedException exception =
        new GraphQlUnauthorizedException("forbidden", 403, Collections.emptyMap(), null);

    assertThrows(GraphQlUnauthorizedException.class, () -> retryProbe.graphqlCall(exception));

    assertEquals(1, retryProbe.getInvocationCount());
  }

  @Test
  void graphqlGlobal_shouldRetryOnlyTransientGraphQlErrors() {
    GraphQlRateLimitException rateLimitException =
        new GraphQlRateLimitException("rate limited", 429, Collections.emptyMap(), null);
    final GraphQlTransientException transientException =
        new GraphQlTransientException("unavailable", 503, Collections.emptyMap(), null);
    final GraphQlTransportException transportException =
        new GraphQlTransportException("connection failed", new IOException("timeout"));

    assertThrows(GraphQlRateLimitException.class, () -> retryProbe.graphqlCall(rateLimitException));
    assertEquals(2, retryProbe.getInvocationCount());

    retryProbe.reset();
    assertThrows(GraphQlTransientException.class, () -> retryProbe.graphqlCall(transientException));
    assertEquals(2, retryProbe.getInvocationCount());

    retryProbe.reset();
    assertThrows(GraphQlTransportException.class, () -> retryProbe.graphqlCall(transportException));
    assertEquals(2, retryProbe.getInvocationCount());
  }

  @Test
  void graphqlOperationPolicies_shouldNotRetryBaseRemoteErrors() {
    GraphQlRemoteException exception = new GraphQlRemoteException("validation failed");

    assertThrows(GraphQlRemoteException.class, () -> retryProbe.graphqlCredentialsCall(exception));
    assertEquals(1, retryProbe.getInvocationCount());

    retryProbe.reset();
    assertThrows(GraphQlRemoteException.class, () -> retryProbe.graphqlProjectsCall(exception));
    assertEquals(1, retryProbe.getInvocationCount());

    retryProbe.reset();
    assertThrows(GraphQlRemoteException.class, () -> retryProbe.graphqlCoalitionCall(exception));
    assertEquals(1, retryProbe.getInvocationCount());
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class RetryPolicyConfiguration {

    @Bean
    RetryProbe retryProbe() {
      return new RetryProbe();
    }
  }

  static class RetryProbe {
    private final AtomicInteger invocationCount = new AtomicInteger();

    @Retry(name = "externalGlobal")
    public void externalCall(RuntimeException exception) {
      invokeAndThrow(exception);
    }

    @Retry(name = "participantProfile")
    public void participantProfileCall(RuntimeException exception) {
      invokeAndThrow(exception);
    }

    @Retry(name = "graphqlGlobal")
    public void graphqlCall(RuntimeException exception) {
      invokeAndThrow(exception);
    }

    @Retry(name = "graphqlCredentials")
    public void graphqlCredentialsCall(RuntimeException exception) {
      invokeAndThrow(exception);
    }

    @Retry(name = "graphqlProjects")
    public void graphqlProjectsCall(RuntimeException exception) {
      invokeAndThrow(exception);
    }

    @Retry(name = "graphqlCoalition")
    public void graphqlCoalitionCall(RuntimeException exception) {
      invokeAndThrow(exception);
    }

    private void invokeAndThrow(RuntimeException exception) {
      invocationCount.incrementAndGet();
      throw exception;
    }

    void reset() {
      invocationCount.set(0);
    }

    int getInvocationCount() {
      return invocationCount.get();
    }
  }
}
