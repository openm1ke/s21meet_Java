package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.izpz.dto.NotifyRequest;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.client.BotClient;
import ru.izpz.edu.service.NotifyService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifySchedulerTest {

    @Mock
    private NotifyService notifyService;

    @Mock
    private BotClient botClient;

    @Mock
    private SchedulerMetricsService schedulerMetricsService;

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
        verify(schedulerMetricsService).recordNotifyRecipients("notify_poller", 1, 1);
        verify(schedulerMetricsService).recordExternalApiSuccess("notify_poller", "bot_api", "notify");

        NotifyRequest request = requestCaptor.getValue();
        assertEquals(changes, request.getChanges());
    }

    @Test
    void poll_shouldRecordFailedStatus_whenNotifyThrows() {
        List<StatusChange> changes = List.of(new StatusChange("login", true, List.of("123")));
        when(notifyService.computeAndPersistChanges()).thenReturn(changes);
        doThrow(new RuntimeException("boom")).when(botClient).notify(any(NotifyRequest.class));

        assertThrows(RuntimeException.class, () -> scheduler.poll());

        verify(schedulerMetricsService).recordNotifyRecipients("notify_poller", 1, 1);
        verify(schedulerMetricsService).recordExternalApiError(eq("notify_poller"), eq("bot_api"), eq("notify"), any(RuntimeException.class));
    }

    @Test
    void poll_shouldCountUniqueUsersAndDeliveries_whenTelegramIdsOverlap() {
        List<StatusChange> changes = List.of(
            new StatusChange("login1", true, List.of("100", "200", "300")),
            new StatusChange("login2", false, List.of("200", "300", "400"))
        );
        when(notifyService.computeAndPersistChanges()).thenReturn(changes);

        scheduler.poll();

        verify(schedulerMetricsService).recordNotifyRecipients("notify_poller", 4, 6);
        verify(botClient).notify(any(NotifyRequest.class));
    }

    @Test
    void poll_shouldSkipBotNotify_whenDeliveryDisabled() {
        ReflectionTestUtils.setField(scheduler, "notifyDeliveryEnabled", false);
        List<StatusChange> changes = List.of(new StatusChange("login", true, List.of("123")));
        when(notifyService.computeAndPersistChanges()).thenReturn(changes);

        scheduler.poll();

        verify(notifyService).computeAndPersistChanges();
        verify(schedulerMetricsService).recordNotifyRecipients("notify_poller", 1, 1);
        verifyNoInteractions(botClient);
        verify(schedulerMetricsService, never()).recordExternalApiSuccess(anyString(), anyString(), anyString());
        verify(schedulerMetricsService, never()).recordExternalApiError(anyString(), anyString(), anyString(), any());
    }
}
