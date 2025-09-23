package ru.izpz.edu.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.edu.client.BotClient;
import ru.izpz.dto.NotifyRequest;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.service.NotifyService;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notify.scheduler.enabled", havingValue = "true")
public class NotifyScheduler {

    private final NotifyService notifyService;
    private final BotClient botClient;

    @Scheduled(fixedDelayString = "${notify.scheduler.fixed-delay:PT30S}")
    public void poll() {
        List<StatusChange> changes = notifyService.computeAndPersistChanges();
        var request = NotifyRequest.builder()
                .changes(changes)
                .build();
        botClient.notify(request);
    }
}
