package ru.izpz.edu.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.edu.model.Friends;
import ru.izpz.edu.model.Online;
import ru.izpz.edu.repository.FriendsRepository;
import ru.izpz.edu.repository.OnlineRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "notify.service.enabled", havingValue = "true", matchIfMissing = true)
public class NotifyService {

    private final FriendsRepository friendsRepository;
    private final WorkplaceRepository workplaceRepository;
    private final OnlineRepository onlineRepository;
    private final MessageSender messageSender;

    public NotifyService(FriendsRepository friendsRepository, WorkplaceRepository workplaceRepository, OnlineRepository onlineRepository, MessageSender messageSender) {
        this.friendsRepository = friendsRepository;
        this.workplaceRepository = workplaceRepository;
        this.onlineRepository = onlineRepository;
        this.messageSender = messageSender;
    }

    public void startNotificationProcess() {
        List<String> subscribeLogins = friendsRepository.findDistinctLogins();

        for (String login : subscribeLogins) {
            boolean inCampus = workplaceRepository.existsByLogin(login);
            Online onlineRecord = getOrCreateOnlineRecord(login);
            boolean currentOnlineStatus = onlineRecord.getIsOnline();

            if (inCampus && !currentOnlineStatus) {
                updateStatusAndNotify(login, onlineRecord, true);
            } else if (!inCampus && currentOnlineStatus) {
                updateStatusAndNotify(login, onlineRecord, false);
            }
        }
    }

    /**
     * Возвращает существующую запись о состоянии пользователя или создаёт новую, если её нет.
     */
    private Online getOrCreateOnlineRecord(String login) {
        return onlineRepository.findByLogin(login)
                .orElseGet(() -> {
                    Online newRecord = new Online();
                    newRecord.setId(UUID.randomUUID());
                    newRecord.setLogin(login);
                    newRecord.setIsOnline(false);
                    return newRecord;
                });
    }

    /**
     * Обновляет статус пользователя, сохраняет запись и отправляет уведомление.
     *
     * @param login       логин пользователя
     * @param onlineRecord запись о состоянии пользователя
     * @param newStatus   новый статус: true - online, false - offline
     */
    private void updateStatusAndNotify(String login, Online onlineRecord, boolean newStatus) {
        Online freshRecord = onlineRepository.findByLogin(login).orElse(onlineRecord);
        freshRecord.setIsOnline(newStatus);
        onlineRepository.saveAndFlush(freshRecord);
        sendNotification(login, newStatus);
    }

    /**
     * Получает подписчиков для пользователя и отправляет соответствующее уведомление.
     *
     * @param login  логин пользователя
     * @param online новый статус, определяющий тип уведомления
     */
    private void sendNotification(String login, boolean online) {
        List<Friends> subscribers = friendsRepository.findByLoginAndIsSubscribeTrue(login);
        List<String> telegramIds = subscribers.stream()
                .map(Friends::getTelegramId)
                .toList();
        if (online) {
            messageSender.sendOnlineNotification(login, telegramIds);
        } else {
            messageSender.sendOfflineNotification(login, telegramIds);
        }
    }
}
