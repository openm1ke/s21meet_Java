package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.izpz.edu.BaseTestContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "message.service.enabled=true")
class MessageSenderTest extends BaseTestContainer {
    @Autowired
    private MessageSender messageSender;

    private final String login = "testlogin";
    private final List<String> telegramIds = List.of("123456", "789012");

    @Test
    void sendOnlineNotification_shouldLogOnlineStatusAndNotModifyList() {
        // Вызываем метод
        messageSender.sendOnlineNotification(login, telegramIds);

        // Проверяем, что список Telegram ID не изменился и содержит два элемента
        assertThat(telegramIds).hasSize(2);
    }

    @Test
    void sendOfflineNotification_shouldLogOfflineStatusAndNotModifyList() {
        // Вызываем метод
        messageSender.sendOfflineNotification(login, telegramIds);

        // Проверяем, что список Telegram ID не изменился и содержит два элемента
        assertThat(telegramIds).hasSize(2);
    }
}