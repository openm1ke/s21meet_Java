package ru.izpz.edu.client;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.ApiResponse;

import java.lang.reflect.Type;

/**
 * Единая точка rate limit/retry для всех REST-вызовов платформы через OpenAPI client.
 */
public class PlatformApiClient extends ApiClient {

    public PlatformApiClient(OkHttpClient httpClient) {
        super(httpClient);
    }

    @Override
    @RateLimiter(name = "externalGlobal")
    public <T> ApiResponse<T> execute(Call call) throws ApiException {
        return super.execute(call);
    }

    @Override
    @RateLimiter(name = "externalGlobal")
    public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
        return super.execute(call, returnType);
    }
}
