package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.EventClient;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.edu.service.EventService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSchedulerTest {

    @Mock
    private EventClient eventClient;

    @Mock
    private EventService eventService;

    @Mock
    private SchedulerMetricsService schedulerMetricsService;

    @InjectMocks
    private EventScheduler scheduler;

    @Test
    void scheduleEvents_shouldSaveEvents_whenClientReturnsEvents() throws Exception {
        List<EventV1DTO> events = List.of(new EventV1DTO());
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenReturn(events);
        when(eventService.saveEvents(events)).thenReturn(new EventService.SaveEventsStats(1, 0));

        scheduler.scheduleEvents();

        ArgumentCaptor<java.time.OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(java.time.OffsetDateTime.class);
        ArgumentCaptor<java.time.OffsetDateTime> toCaptor = ArgumentCaptor.forClass(java.time.OffsetDateTime.class);
        ArgumentCaptor<Long> limitCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> offsetCaptor = ArgumentCaptor.forClass(Long.class);

        verify(eventClient).getEvents(fromCaptor.capture(), toCaptor.capture(), isNull(), limitCaptor.capture(), offsetCaptor.capture());
        verify(eventService).saveEvents(events);
        verify(schedulerMetricsService).recordEventsSaved("event_parser", 1, 0);
        verify(schedulerMetricsService).recordExternalApiSuccess("event_parser", "event_api", "get_events");

        assertNotNull(fromCaptor.getValue());
        assertNotNull(toCaptor.getValue());
        assertTrue(toCaptor.getValue().isAfter(fromCaptor.getValue()));

        long days = Duration.between(fromCaptor.getValue(), toCaptor.getValue()).toDays();
        assertEquals(7L, days);

        assertEquals(50L, limitCaptor.getValue());
        assertEquals(0L, offsetCaptor.getValue());
    }

    @Test
    void scheduleEvents_shouldThrow_whenClientThrows() throws Exception {
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenThrow(new ApiException("boom"));

        assertThrows(ApiException.class, () -> scheduler.scheduleEvents());

        verify(eventService, never()).saveEvents(anyList());
        verify(schedulerMetricsService).recordExternalApiError(eq("event_parser"), eq("event_api"), eq("get_events"), any(ApiException.class));
    }

    @Test
    void scheduleEvents_shouldThrow_whenUnexpectedExceptionHappens() throws Exception {
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenThrow(new NullPointerException("boom"));

        assertThrows(NullPointerException.class, () -> scheduler.scheduleEvents());

        verify(eventService, never()).saveEvents(anyList());
        verify(schedulerMetricsService).recordExternalApiError(eq("event_parser"), eq("event_api"), eq("get_events"), any(NullPointerException.class));
    }

    @Test
    void scheduleEvents_shouldNotRecordExternalApiError_whenSavingFailsAfterSuccessfulFetch() throws Exception {
        List<EventV1DTO> events = List.of(new EventV1DTO());
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenReturn(events);
        when(eventService.saveEvents(events)).thenThrow(new RuntimeException("db boom"));

        assertThrows(RuntimeException.class, () -> scheduler.scheduleEvents());

        verify(schedulerMetricsService).recordExternalApiSuccess("event_parser", "event_api", "get_events");
        verify(schedulerMetricsService, never()).recordExternalApiError(eq("event_parser"), eq("event_api"), eq("get_events"), any());
        verify(schedulerMetricsService, never()).recordEventsSaved(anyString(), anyLong(), anyLong());
    }

    @Test
    void scheduleEvents_shouldNotRecordSavedMetrics_whenSaveStatsIsNull() throws Exception {
        List<EventV1DTO> events = List.of(new EventV1DTO());
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenReturn(events);
        when(eventService.saveEvents(events)).thenReturn(null);

        scheduler.scheduleEvents();

        verify(schedulerMetricsService).recordExternalApiSuccess("event_parser", "event_api", "get_events");
        verify(schedulerMetricsService, never()).recordEventsSaved(anyString(), anyLong(), anyLong());
    }
}
