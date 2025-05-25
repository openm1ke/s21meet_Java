package ru.izpz.edu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "message.service.enabled", havingValue = "true", matchIfMissing = true)
public class MessageSender {

    public void sendOnlineNotification(String login, List<String> telegramIds) {
        log.info("{} online", login);
        telegramIds.forEach(log::info);
    }

    public void sendOfflineNotification(String login, List<String> telegramIds) {
        log.info("{} offline", login);
        telegramIds.forEach(log::info);
    }
}
