package ru.izpz.edu.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.EventApi;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.dto.model.EventsV1DTO;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventClientTest {

    @Mock
    private EventApi eventApi;

    @InjectMocks
    private EventClient eventClient;

    @Test
    void getEvents_shouldThrow_whenResponseNull() throws Exception {
        when(eventApi.getEvents(any(), any(), any(), any(), any())).thenReturn(null);

        assertThrows(ApiException.class, () -> eventClient.getEvents(OffsetDateTime.now(), OffsetDateTime.now(), null, 50L, 0L));
    }

    @Test
    void getEvents_shouldReturnEvents_whenResponseNotNull() throws Exception {
        EventV1DTO e = new EventV1DTO();
        EventsV1DTO resp = new EventsV1DTO();
        resp.setEvents(List.of(e));

        when(eventApi.getEvents(any(), any(), any(), any(), any())).thenReturn(resp);

        List<EventV1DTO> result = eventClient.getEvents(OffsetDateTime.now(), OffsetDateTime.now(), null, 50L, 0L);

        assertEquals(1, result.size());
        assertSame(e, result.getFirst());
    }
}
