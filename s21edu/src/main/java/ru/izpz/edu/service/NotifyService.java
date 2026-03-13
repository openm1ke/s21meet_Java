package ru.izpz.edu.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
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
        if (logins.isEmpty()) {
            log.debug("computeAndPersistChanges: no tracked logins");
            return List.of();
        }

        List<StatusChange> changes = new ArrayList<>();
        int becameOnline = 0;
        int becameOffline = 0;

        for (String login : logins) {
            boolean inCampus = workplaceRepository.existsByLogin(login);
            Optional<Online> opt = onlineRepository.findByLogin(login);

            if (inCampus) {
                boolean shouldMarkOnline = opt.isEmpty() || Boolean.FALSE.equals(opt.get().getIsOnline());
                if (shouldMarkOnline) {
                    persistOnlineStatus(opt, login, true);
                    addChangeIfSubscribers(changes, login, true);
                    becameOnline++;
                }
            } else {
                if (opt.isPresent() && Boolean.TRUE.equals(opt.get().getIsOnline())) {
                    persistOnlineStatus(opt, login, false);
                    addChangeIfSubscribers(changes, login, false);
                    becameOffline++;
                }
            }
        }
        long recipients = changes.stream()
            .map(StatusChange::telegramIds)
            .filter(Objects::nonNull)
            .mapToLong(List::size)
            .sum();
        log.info(
            "computeAndPersistChanges: processedLogins={}, becameOnline={}, becameOffline={}, emittedChanges={}, recipients={}",
            logins.size(),
            becameOnline,
            becameOffline,
            changes.size(),
            recipients
        );
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

    private void persistOnlineStatus(Optional<Online> current, String login, boolean status) {
        try {
            Online entity = current.orElseGet(() -> {
                Online created = new Online();
                created.setLogin(login);
                return created;
            });
            entity.setIsOnline(status);
            onlineRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition while saving online status for login={}: {}", login, e.getMessage());
            onlineRepository.findByLogin(login).ifPresent(existing -> {
                existing.setIsOnline(status);
                onlineRepository.save(existing);
            });
        }
    }
}
