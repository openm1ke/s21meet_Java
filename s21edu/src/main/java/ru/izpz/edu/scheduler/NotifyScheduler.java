package ru.izpz.edu.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.izpz.edu.dto.StatusChange;
import ru.izpz.edu.service.MessageSender;
import ru.izpz.edu.service.NotifyService;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notify.scheduler.enabled", havingValue = "true")
public class NotifyScheduler {

    private final NotifyService notifyService;
    private final MessageSender messageSender;

    @Scheduled(fixedDelayString = "${notify.poll.fixed-delay:PT30S}")
    public void poll() {
        List<StatusChange> changes = notifyService.computeAndPersistChanges();

        for (StatusChange c : changes) {
            if (c.newStatus()) {
                messageSender.sendOnlineNotification(c.login(), c.telegramIds());
            } else {
                messageSender.sendOfflineNotification(c.login(), c.telegramIds());
            }
        }
    }
}
