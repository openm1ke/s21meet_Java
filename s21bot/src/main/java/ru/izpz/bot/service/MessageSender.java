package ru.izpz.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.StatusChange;

import java.util.List;

@Slf4j
@Service
public class MessageSender {

    private void sendOnlineNotification(String login, List<String> telegramIds) {
        log.info("{} online", login);
        telegramIds.forEach(log::info);
    }

    private void sendOfflineNotification(String login, List<String> telegramIds) {
        log.info("{} offline", login);
        telegramIds.forEach(log::info);
    }

    public void sendStatusChanges(List<StatusChange> changes) {
        for (StatusChange c : changes) {
            if (c.newStatus()) {
                this.sendOnlineNotification(c.login(), c.telegramIds());
            } else {
                this.sendOfflineNotification(c.login(), c.telegramIds());
            }
        }
    }
}
