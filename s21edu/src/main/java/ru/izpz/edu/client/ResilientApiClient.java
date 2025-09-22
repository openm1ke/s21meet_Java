package ru.izpz.edu.client;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.ApiResponse;

import java.lang.reflect.Type;

@Slf4j
public class ResilientApiClient extends ApiClient {

    public ResilientApiClient(OkHttpClient client) {
        super(client);
    }

    @RateLimiter(name = "platform")
    @Retry(name = "platform")
    @Override
    public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
        return super.execute(call, returnType);
    }

    @RateLimiter(name = "platform")
    @Retry(name = "platform")
    @Override
    public <T> ApiResponse<T> execute(Call call) throws ApiException {
        return super.execute(call);
    }
}
