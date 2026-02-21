package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.NotifyRequest;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.client.BotClient;
import ru.izpz.edu.service.NotifyService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
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
        when(notifyService.computeAndPersistChanges()).thenReturn(List.of(new StatusChange("login", true, List.of("123"))));

        scheduler.poll();

        verify(botClient).notify(any(NotifyRequest.class));
    }
}
