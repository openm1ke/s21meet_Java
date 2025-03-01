package ru.school21.edu.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.school21.edu.model.Friends;
import ru.school21.edu.model.Online;
import ru.school21.edu.repository.FriendsRepository;
import ru.school21.edu.repository.OnlineRepository;
import ru.school21.edu.repository.WorkplaceRepository;

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
            Online onlineRecord = onlineRepository.findByLogin(login)
                .orElseGet(() -> {
                    Online newRecord = new Online();
                    newRecord.setId(UUID.randomUUID());
                    newRecord.setLogin(login);
                    newRecord.setIsOnline(false);
                    return newRecord;
                });

            if (inCampus && !onlineRecord.getIsOnline()) {
                onlineRecord.setIsOnline(true);
                onlineRepository.save(onlineRecord);

                List<Friends> subscribers = friendsRepository.findByLoginAndIsSubscribeTrue(login);
                List<String> telegramIds = subscribers.stream()
                        .map(Friends::getTelegramId)
                        .toList();
                messageSender.sendOnlineNotification(login, telegramIds);
            } else if (!inCampus && onlineRecord.getIsOnline()) {
                onlineRecord.setIsOnline(false);
                onlineRepository.save(onlineRecord);

                List<Friends> subscribers = friendsRepository.findByLoginAndIsSubscribeTrue(login);
                List<String> telegramIds = subscribers.stream()
                        .map(Friends::getTelegramId)
                        .toList();
                messageSender.sendOfflineNotification(login, telegramIds);
            }
        }
    }
}
