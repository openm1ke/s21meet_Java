package ru.izpz.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ProjectExecutorsRateLimitFilterTest {

  private static final String TARGET_PATH = "/api/projects/executors";
  private static final String POST_METHOD = "POST";
  private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
  private static final String RATE_LIMIT_IP = "77.77.77.77";

  @Test
  void doFilter_shouldAllowWithinLimit() throws Exception {
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 2, Duration.ofMinutes(1));
    MockHttpServletRequest request = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    request.setRemoteAddr("127.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertEquals(request, chain.getRequest());
  }

  @Test
  void doFilter_shouldReturn429WhenLimitExceeded() throws Exception {
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 1, Duration.ofMinutes(1));

    MockHttpServletRequest first = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    first.setRemoteAddr("10.0.0.1");
    MockHttpServletResponse firstResponse = new MockHttpServletResponse();
    filter.doFilter(first, firstResponse, new MockFilterChain());

    MockHttpServletRequest second = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    second.setRemoteAddr("10.0.0.1");
    MockHttpServletResponse secondResponse = new MockHttpServletResponse();
    MockFilterChain secondChain = new MockFilterChain();
    filter.doFilter(second, secondResponse, secondChain);

    assertEquals(429, secondResponse.getStatus());
    assertEquals("{\"message\":\"Too many requests\"}", secondResponse.getContentAsString());
    assertNull(secondChain.getRequest());
  }

  @Test
  void doFilter_shouldResetWindowAfterRefreshPeriod() throws Exception {
    AtomicLong now = new AtomicLong(1_000L);
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 1, Duration.ofMillis(20), now::get);

    MockHttpServletRequest first = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    first.setRemoteAddr("10.0.0.2");
    filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

    now.addAndGet(30L);

    MockHttpServletRequest second = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    second.setRemoteAddr("10.0.0.2");
    MockHttpServletResponse secondResponse = new MockHttpServletResponse();
    MockFilterChain secondChain = new MockFilterChain();
    filter.doFilter(second, secondResponse, secondChain);

    assertEquals(200, secondResponse.getStatus());
    assertEquals(second, secondChain.getRequest());
  }

  @Test
  void doFilter_shouldBypassWhenLimiterDisabled() throws Exception {
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(false, TARGET_PATH, 1, Duration.ofMinutes(1));
    MockHttpServletRequest request = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertEquals(request, chain.getRequest());
  }

  @Test
  void doFilter_shouldBypassForNonTargetPathAndMethod() throws Exception {
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 1, Duration.ofMinutes(1));

    MockHttpServletRequest getRequest = new MockHttpServletRequest("GET", TARGET_PATH);
    MockHttpServletResponse getResponse = new MockHttpServletResponse();
    MockFilterChain getChain = new MockFilterChain();
    filter.doFilter(getRequest, getResponse, getChain);

    MockHttpServletRequest otherPathRequest =
        new MockHttpServletRequest(POST_METHOD, "/api/projects/other");
    MockHttpServletResponse otherPathResponse = new MockHttpServletResponse();
    MockFilterChain otherPathChain = new MockFilterChain();
    filter.doFilter(otherPathRequest, otherPathResponse, otherPathChain);

    assertEquals(200, getResponse.getStatus());
    assertEquals(getRequest, getChain.getRequest());
    assertEquals(200, otherPathResponse.getStatus());
    assertEquals(otherPathRequest, otherPathChain.getRequest());
  }

  @Test
  void doFilter_shouldUsexForwardedForAsRateLimitKey() throws Exception {
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 1, Duration.ofMinutes(1));

    MockHttpServletRequest first = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    first.setRemoteAddr("10.10.10.10");
    first.addHeader(X_FORWARDED_FOR_HEADER, " 1.2.3.4 , 5.6.7.8 ");
    filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

    MockHttpServletRequest second = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    second.setRemoteAddr("20.20.20.20");
    second.addHeader(X_FORWARDED_FOR_HEADER, "1.2.3.4");
    MockHttpServletResponse secondResponse = new MockHttpServletResponse();
    filter.doFilter(second, secondResponse, new MockFilterChain());

    assertEquals(429, secondResponse.getStatus());
  }

  @Test
  void doFilter_shouldCleanupExpiredWindows() throws Exception {
    AtomicLong now = new AtomicLong(5_000L);
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(
            true, TARGET_PATH, 1000, Duration.ofMillis(5), now::get);

    MockHttpServletRequest staleRequest = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    staleRequest.setRemoteAddr("192.168.0.1");
    filter.doFilter(staleRequest, new MockHttpServletResponse(), new MockFilterChain());

    now.addAndGet(20L);

    for (int i = 0; i < 210; i++) {
      MockHttpServletRequest request = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
      request.setRemoteAddr("10.0.0." + i);
      filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    MockHttpServletRequest fresh = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    fresh.setRemoteAddr("192.168.0.1");
    MockHttpServletResponse freshResponse = new MockHttpServletResponse();
    filter.doFilter(fresh, freshResponse, new MockFilterChain());

    assertEquals(200, freshResponse.getStatus());
  }

  @Test
  void doFilter_shouldUseDefaultPathAndMinimalLimitsFromInvalidConstructorArgs() throws Exception {
    AtomicLong now = new AtomicLong(42_000L);
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, "   ", 0, Duration.ZERO, now::get);

    MockHttpServletRequest first = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    first.setRemoteAddr("100.100.100.1");
    filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

    MockHttpServletRequest second = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    second.setRemoteAddr("100.100.100.1");
    MockHttpServletResponse secondResponse = new MockHttpServletResponse();
    filter.doFilter(second, secondResponse, new MockFilterChain());

    assertEquals(429, secondResponse.getStatus());
  }

  @Test
  void doFilter_shouldFallbackToRemoteAddrWhenxForwardedForFirstTokenBlank() throws Exception {
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 1, Duration.ofMinutes(1));

    MockHttpServletRequest first = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    first.setRemoteAddr("55.55.55.55");
    first.addHeader(X_FORWARDED_FOR_HEADER, "   , 1.2.3.4");
    filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

    MockHttpServletRequest second = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    second.setRemoteAddr("55.55.55.55");
    MockHttpServletResponse secondResponse = new MockHttpServletResponse();
    filter.doFilter(second, secondResponse, new MockFilterChain());

    assertEquals(429, secondResponse.getStatus());
  }

  @Test
  void doFilter_shouldUseUnknownKeyWhenRemoteAddrIsNull() throws Exception {
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 1, Duration.ofMinutes(1));

    MockHttpServletRequest first =
        new MockHttpServletRequest(POST_METHOD, TARGET_PATH) {
          @Override
          public String getRemoteAddr() {
            return null;
          }
        };
    filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

    MockHttpServletRequest second =
        new MockHttpServletRequest(POST_METHOD, TARGET_PATH) {
          @Override
          public String getRemoteAddr() {
            return null;
          }
        };
    MockHttpServletResponse secondResponse = new MockHttpServletResponse();
    filter.doFilter(second, secondResponse, new MockFilterChain());

    assertEquals(429, secondResponse.getStatus());
  }

  @Test
  void doFilter_shouldIncrementCounterWhenWithinWindowAndBelowLimit() throws Exception {
    final ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 3, Duration.ofMinutes(1));

    MockHttpServletRequest first = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    first.setRemoteAddr(RATE_LIMIT_IP);
    MockHttpServletRequest second = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    second.setRemoteAddr(RATE_LIMIT_IP);
    MockHttpServletRequest third = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    third.setRemoteAddr(RATE_LIMIT_IP);
    MockHttpServletRequest fourth = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    fourth.setRemoteAddr(RATE_LIMIT_IP);

    MockHttpServletResponse firstResponse = new MockHttpServletResponse();
    MockHttpServletResponse secondResponse = new MockHttpServletResponse();
    MockHttpServletResponse thirdResponse = new MockHttpServletResponse();
    MockHttpServletResponse fourthResponse = new MockHttpServletResponse();

    filter.doFilter(first, firstResponse, new MockFilterChain());
    filter.doFilter(second, secondResponse, new MockFilterChain());
    filter.doFilter(third, thirdResponse, new MockFilterChain());
    filter.doFilter(fourth, fourthResponse, new MockFilterChain());

    assertEquals(200, firstResponse.getStatus());
    assertEquals(200, secondResponse.getStatus());
    assertEquals(200, thirdResponse.getStatus());
    assertEquals(429, fourthResponse.getStatus());
  }

  @Test
  void doFilter_shouldIgnoreBlankxForwardedForAndUseRemoteAddr() throws Exception {
    ProjectExecutorsRateLimitFilter filter =
        new ProjectExecutorsRateLimitFilter(true, TARGET_PATH, 1, Duration.ofMinutes(1));

    MockHttpServletRequest first = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    first.setRemoteAddr("66.66.66.66");
    first.addHeader(X_FORWARDED_FOR_HEADER, "   ");
    filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

    MockHttpServletRequest second = new MockHttpServletRequest(POST_METHOD, TARGET_PATH);
    second.setRemoteAddr("66.66.66.66");
    MockHttpServletResponse secondResponse = new MockHttpServletResponse();
    filter.doFilter(second, secondResponse, new MockFilterChain());

    assertEquals(429, secondResponse.getStatus());
  }
}
