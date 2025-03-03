package ru.school21.edu.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.school21.edu.BaseTestContainer;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestPropertySource(properties = "message.service.enabled=true")
class MessageSenderTest extends BaseTestContainer {
    @Autowired
    private MessageSender messageSender;

    String login = "testlogin";
    List<String> telegramIds = List.of("123456", "789012");

    @Test
    void sendOnlineNotification() {
        messageSender.sendOnlineNotification(login, telegramIds);
        assertThat(telegramIds.size()).isEqualTo(2);
    }

    @Test
    void sendOfflineNotification() {
        messageSender.sendOfflineNotification(login, telegramIds);
        assertThat(telegramIds.size()).isEqualTo(2);
    }
}