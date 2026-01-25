package ru.izpz.edu.client;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.EventApi;
import ru.izpz.dto.model.EventV1DTO;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventClient {

    private final EventApi eventApi;

    @RateLimiter(name = "platform")
    @Retry(name = "platform")
    public List<EventV1DTO> getEvents(OffsetDateTime from, OffsetDateTime to, String type, Long limit, Long offset) throws ApiException {
        var response = eventApi.getEvents(from, to, type, limit, offset);
        if (response == null) {
            log.warn("API вернул null для событий");
            throw new ApiException("API вернул null для событий");
        }
        return response.getEvents();
    }
}
