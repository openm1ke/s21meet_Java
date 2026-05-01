package ru.izpz.edu.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.dto.model.EventsV1DTO;
import ru.izpz.edu.exception.PlatformClientException;

@ExtendWith(MockitoExtension.class)
class EventClientTest {

  @Mock private PlatformApiFacade platformApi;

  @InjectMocks private EventClient eventClient;

  @Test
  void getEvents_shouldThrow_whenResponseNull() {
    when(platformApi.getEvents(any(), any(), any(), any(), any())).thenReturn(null);
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = from.plusHours(1);

    assertThrows(
        PlatformClientException.class, () -> eventClient.getEvents(from, to, null, 50L, 0L));
  }

  @Test
  void getEvents_shouldReturnEvents_whenResponseNotNull() {
    EventV1DTO e = new EventV1DTO();
    EventsV1DTO resp = new EventsV1DTO();
    resp.setEvents(List.of(e));

    when(platformApi.getEvents(any(), any(), any(), any(), any())).thenReturn(resp);

    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = from.plusHours(1);
    List<EventV1DTO> result = eventClient.getEvents(from, to, null, 50L, 0L);

    assertEquals(1, result.size());
    assertSame(e, result.getFirst());
  }

  @Test
  void getEvents_shouldWrapRuntimeException() {
    when(platformApi.getEvents(any(), any(), any(), any(), any()))
        .thenThrow(new NullPointerException("boom"));
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = from.plusHours(1);

    PlatformClientException ex =
        assertThrows(
            PlatformClientException.class, () -> eventClient.getEvents(from, to, null, 50L, 0L));
    assertNotNull(ex.getCause());
  }
}
