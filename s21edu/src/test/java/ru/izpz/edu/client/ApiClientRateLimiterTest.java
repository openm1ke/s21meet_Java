package ru.izpz.edu.client;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryMetricsAutoConfiguration;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ActiveProfiles;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.ApiResponse;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = { ApiClientRateLimiterTest.TestConfig.class },
    properties = {
        "resilience4j.ratelimiter.instances.testApi.limitForPeriod=2",
        "resilience4j.ratelimiter.instances.testApi.limitRefreshPeriod=1s",
        "resilience4j.ratelimiter.instances.testApi.timeoutDuration=0"
    }
)
@ActiveProfiles("test")
class ApiClientRateLimiterTest {

    private static final String RL_NAME = "testApi";

    @Configuration
    @ImportAutoConfiguration({
        RetryMetricsAutoConfiguration.class,
        RateLimiterAutoConfiguration.class
    })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        public ResilientApiClient apiClient() {
            return new ResilientApiClient();
        }
    }

    public static class ResilientApiClient extends ApiClient {
        @Override
        @RateLimiter(name = RL_NAME)
        public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
            return super.execute(call, returnType);
        }

        @Override
        @RateLimiter(name = RL_NAME)
        public <T> ApiResponse<T> execute(Call call) throws ApiException {
            return super.execute(call);
        }
    }

    @Autowired
    RateLimiterRegistry rateLimiterRegistry;

    @Autowired
    private ResilientApiClient apiClient;

    @BeforeEach
    void resetRateLimiter() {
        rateLimiterRegistry.remove(RL_NAME);

        RateLimiterConfig cfg = rateLimiterRegistry.getConfiguration(RL_NAME)
                .orElse(RateLimiterConfig.custom()
                        .limitForPeriod(2)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ZERO)
                        .build());

        rateLimiterRegistry.rateLimiter(RL_NAME, cfg);
    }

    @Test
    void allows_first_two_calls_and_blocks_next_three() throws Exception {
        List<Throwable> thrown = new ArrayList<>();

        ApiResponse<Void> r1 = apiClient.execute(mockOkCall200(), null);
        ApiResponse<Void> r2 = apiClient.execute(mockOkCall200(), null);
        assertEquals(200, r1.getStatusCode());
        assertEquals(200, r2.getStatusCode());

        for (int i = 0; i < 3; i++) {
            final int callNumber = i + 1;
            Call call = mockOkCall200();
            RequestNotPermitted ex = assertThrows(RequestNotPermitted.class, 
                () -> apiClient.execute(call, null),
                "Должно было кинуть RequestNotPermitted на вызове #" + callNumber);
            thrown.add(ex);
        }
        assertEquals(3, thrown.size(), "Все три лишних вызова должны быть заблокированы");
    }

    @Test
    void refreshes_after_window() throws Exception {
        saturateRateLimiter();

        Duration refreshPeriod = rateLimiterRegistry
                .rateLimiter(RL_NAME)
                .getRateLimiterConfig()
                .getLimitRefreshPeriod();
        Thread.sleep(refreshPeriod.plusMillis(200).toMillis());

        ApiResponse<Void> a = apiClient.execute(mockOkCall200(), null);
        ApiResponse<Void> b = apiClient.execute(mockOkCall200(), null);
        assertEquals(200, a.getStatusCode());
        assertEquals(200, b.getStatusCode());
    }

    private void saturateRateLimiter() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            try {
                apiClient.execute(mockOkCall200(), null);
            } catch (RequestNotPermitted ignored) {
                return;
            }
        }
        fail("Rate limiter should deny requests after limit is reached");
    }

    private static Call mockOkCall200() throws IOException {
        Call call = mock(Call.class);
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost/ok").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("{}", MediaType.get("application/json")))
                .build();
        when(call.execute()).thenReturn(response);
        return call;
    }
}
