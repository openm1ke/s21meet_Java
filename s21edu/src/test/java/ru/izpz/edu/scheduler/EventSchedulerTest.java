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

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @InjectMocks
    private EventScheduler scheduler;

    @Test
    void scheduleEvents_shouldSaveEvents_whenClientReturnsEvents() throws Exception {
        List<EventV1DTO> events = List.of(new EventV1DTO());
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenReturn(events);

        scheduler.scheduleEvents();

        ArgumentCaptor<java.time.OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(java.time.OffsetDateTime.class);
        ArgumentCaptor<java.time.OffsetDateTime> toCaptor = ArgumentCaptor.forClass(java.time.OffsetDateTime.class);
        ArgumentCaptor<Long> limitCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> offsetCaptor = ArgumentCaptor.forClass(Long.class);

        verify(eventClient).getEvents(fromCaptor.capture(), toCaptor.capture(), isNull(), limitCaptor.capture(), offsetCaptor.capture());
        verify(eventService).saveEvents(events);

        assertNotNull(fromCaptor.getValue());
        assertNotNull(toCaptor.getValue());
        assertTrue(toCaptor.getValue().isAfter(fromCaptor.getValue()));

        long days = Duration.between(fromCaptor.getValue(), toCaptor.getValue()).toDays();
        assertEquals(7L, days);

        assertEquals(50L, limitCaptor.getValue());
        assertEquals(0L, offsetCaptor.getValue());
    }

    @Test
    void scheduleEvents_shouldNotThrow_whenClientThrows() throws Exception {
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenThrow(new ApiException("boom"));

        scheduler.scheduleEvents();

        verify(eventService, never()).saveEvents(anyList());
    }
}
