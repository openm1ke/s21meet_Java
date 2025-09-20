package ru.izpz.edu.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.model.Friends;
import ru.izpz.edu.model.Online;
import ru.izpz.edu.repository.FriendsRepository;
import ru.izpz.edu.repository.OnlineRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(name = "notify.service.enabled", havingValue = "true")
public class NotifyService {

    private final FriendsRepository friendsRepository;
    private final WorkplaceRepository workplaceRepository;
    private final OnlineRepository onlineRepository;

    public NotifyService(FriendsRepository friendsRepository, WorkplaceRepository workplaceRepository, OnlineRepository onlineRepository) {
        this.friendsRepository = friendsRepository;
        this.workplaceRepository = workplaceRepository;
        this.onlineRepository = onlineRepository;
    }

    @Transactional
    public List<StatusChange> computeAndPersistChanges() {
        List<String> logins = friendsRepository.findDistinctLogins();
        if (logins.isEmpty()) return List.of();

        List<StatusChange> changes = new ArrayList<>();

        for (String login : logins) {
            boolean inCampus = workplaceRepository.existsByLogin(login);
            Optional<Online> opt = onlineRepository.findByLogin(login);

            if (inCampus) {
                if (opt.isEmpty()) {
                    Online o = new Online();
                    o.setLogin(login);
                    o.setIsOnline(true);
                    onlineRepository.save(o);
                    addChangeIfSubscribers(changes, login, true);
                } else if (!opt.get().getIsOnline()) {
                    Online o = opt.get();
                    o.setIsOnline(true);
                    onlineRepository.save(o);
                    addChangeIfSubscribers(changes, login, true);
                }
            } else {
                if (opt.isPresent() && opt.get().getIsOnline()) {
                    Online o = opt.get();
                    o.setIsOnline(false);
                    onlineRepository.save(o);
                    addChangeIfSubscribers(changes, login, false);
                }
            }
        }
        return changes;
    }

    private void addChangeIfSubscribers(List<StatusChange> changes, String login, boolean newStatus) {
        List<String> ids = friendsRepository.findByLoginAndIsSubscribeTrue(login).stream()
                .map(Friends::getTelegramId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!ids.isEmpty()) changes.add(new StatusChange(login, newStatus, ids));
    }
}
