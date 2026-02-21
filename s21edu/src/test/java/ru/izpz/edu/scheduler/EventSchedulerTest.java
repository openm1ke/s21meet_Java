package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.EventClient;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.edu.service.EventService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
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
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenReturn(List.of(new EventV1DTO()));

        scheduler.scheduleEvents();

        verify(eventService).saveEvents(anyList());
    }

    @Test
    void scheduleEvents_shouldNotThrow_whenClientThrows() throws Exception {
        when(eventClient.getEvents(any(), any(), any(), anyLong(), anyLong())).thenThrow(new ApiException("boom"));

        scheduler.scheduleEvents();

        verify(eventService, never()).saveEvents(anyList());
    }
}
