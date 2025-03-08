package ru.school21.edu.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import ru.school21.edu.BaseTestContainer;
import ru.school21.edu.config.NotifyServiceConfig;
import ru.school21.edu.model.Online;
import ru.school21.edu.repository.OnlineRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest (classes = {ru.school21.edu.Application.class, NotifyServiceConfig.class})
@Sql(scripts = {"/friends_import.sql", "/online_import.sql", "/workplace_import.sql"})
@TestPropertySource(properties = {"notify.service.enabled=true", "message.service.enabled = true"})
@Transactional
class NotifyServiceTest extends BaseTestContainer {

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private OnlineRepository onlineRepository;

    @Autowired
    private MessageSender messageSender;

    @Test
    void testStartNotificationProcess_sendsOnlineNotification() {
        Optional<Online> lucankriOpt = onlineRepository.findByLogin("lucankri");
        assertTrue(lucankriOpt.isPresent(), "Запись для lucankri должна присутствовать");
        assertFalse(lucankriOpt.get().getIsOnline(), "Исходный статус для lucankri должен быть FALSE");

        notifyService.startNotificationProcess();

        // Проверяем, что для "lucankri" вызвано уведомление об online
        verify(messageSender, atLeastOnce())
                .sendOnlineNotification("lucankri", java.util.List.of("703226616"));

        Online updatedLucankri = onlineRepository.findByLogin("lucankri").orElseThrow();
        assertTrue(updatedLucankri.getIsOnline(), "Статус для lucankri должен стать TRUE");
    }

    @Test
    void testStartNotificationProcess_noNotificationWhenNoStateChange() {
        String login = "elevante";
        Optional<Online> onlineOpt = onlineRepository.findByLogin(login);
        assertTrue(onlineOpt.isPresent(), "Запись для elevante должна присутствовать");
        assertTrue(onlineOpt.get().getIsOnline(), "Статус для elevante должен быть TRUE");

        notifyService.startNotificationProcess();

        verify(messageSender, never()).sendOnlineNotification(eq(login), anyList());
        verify(messageSender, never()).sendOfflineNotification(eq(login), anyList());
    }

    @Test
    void testStartNotificationProcess_noNotificationForAbsentOnlineRecord() {
        String login = "scrimgew";
        // Проверяем, что в таблице online для "scrimgew" записи нет
        Optional<Online> onlineOpt = onlineRepository.findByLogin(login);
        assertTrue(onlineOpt.isEmpty(), "Записи для scrimgew в online не должно быть");

        notifyService.startNotificationProcess();

        verify(messageSender, never()).sendOnlineNotification(eq(login), anyList());
        verify(messageSender, never()).sendOfflineNotification(eq(login), anyList());

        // После выполнения процесса записи по-прежнему не должно появиться
        Optional<Online> onlineAfter = onlineRepository.findByLogin(login);
        assertTrue(onlineAfter.isEmpty(), "Запись для scrimgew в online не должна появиться");
    }

}