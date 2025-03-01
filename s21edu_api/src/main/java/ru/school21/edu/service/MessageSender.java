package ru.school21.edu.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "message.service.enabled", havingValue = "true", matchIfMissing = true)
public class MessageSender {

    public void sendOnlineNotification(String login, List<String> telegramIds) {
        // TODO: send message online to all subscribers in telegram
        System.out.println(login + " online");
    }

    public void sendOfflineNotification(String login, List<String> telegramIds) {
        // TODO: send message offline to all subscribers in telegram
        System.out.println(login + " offline");
    }
}
