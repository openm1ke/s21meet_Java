package ru.izpz.edu.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.github.resilience4j.retry.annotation.Retry;
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

    @Retry(name = "platform")
    public List<EventV1DTO> getEvents(OffsetDateTime from, OffsetDateTime to, String type, Long limit, Long offset) throws ApiException {
        try {
            var response = eventApi.getEvents(from, to, type, limit, offset);
            if (response == null) {
                log.warn("API вернул пустой ответ для событий");
                throw new ApiException("API вернул пустой ответ для событий");
            }
            return response.getEvents();
        } catch (RuntimeException e) {
            log.error("Неожиданная ошибка при получении событий", e);
            throw new ApiException("Unexpected error while fetching events", e, 500, null, null);
        }
    }
}
