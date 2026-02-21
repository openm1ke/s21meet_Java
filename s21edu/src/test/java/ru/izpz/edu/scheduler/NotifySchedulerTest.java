package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.NotifyRequest;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.client.BotClient;
import ru.izpz.edu.service.NotifyService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifySchedulerTest {

    @Mock
    private NotifyService notifyService;

    @Mock
    private BotClient botClient;

    @InjectMocks
    private NotifyScheduler scheduler;

    @Test
    void poll_shouldSendNotification() {
        List<StatusChange> changes = List.of(new StatusChange("login", true, List.of("123")));
        when(notifyService.computeAndPersistChanges()).thenReturn(changes);

        scheduler.poll();

        ArgumentCaptor<NotifyRequest> requestCaptor = ArgumentCaptor.forClass(NotifyRequest.class);
        verify(notifyService).computeAndPersistChanges();
        verify(botClient).notify(requestCaptor.capture());

        NotifyRequest request = requestCaptor.getValue();
        assertEquals(changes, request.getChanges());
    }
}
