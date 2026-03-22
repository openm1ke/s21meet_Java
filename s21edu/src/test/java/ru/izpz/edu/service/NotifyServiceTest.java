package ru.izpz.edu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ru.izpz.edu.BaseTestH2;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.model.Online;
import ru.izpz.edu.repository.FriendsRepository;
import ru.izpz.edu.repository.OnlineRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Sql({"/friends_import.sql", "/online_import.sql", "/workplace_import.sql"})
@Import({NotifyService.class})
class NotifyServiceTest extends BaseTestH2 {

    @Autowired
    NotifyService notifyService;
    @Autowired
    OnlineRepository onlineRepository;
    @Autowired
    FriendsRepository friendsRepository;
    @Autowired
    WorkplaceRepository workplaceRepository;

    @BeforeEach
    void ensureOnlineRowsExistForAllSubscriptions() {
        // имитируем «создание online=false при подписке»
        Set<String> subscribed = new HashSet<>(friendsRepository.findDistinctLogins());
        for (String login : subscribed) {
            onlineRepository.findByLogin(login).orElseGet(() -> {
                Online o = new Online();
                o.setLogin(login);
                o.setIsOnline(false);
                return onlineRepository.save(o);
            });
        }
    }

    @Test
    void computeAndPersistChanges_onlineFor_lucankri_updatesDb_andReturnsChange() {
        // pre: в workplace есть lucankri, а в online у него false (после @BeforeEach)
        Online before = onlineRepository.findByLogin("lucankri").orElseThrow();
        assertFalse(before.getIsOnline(), "Исходный статус lucankri должен быть FALSE");

        List<StatusChange> changes = notifyService.computeAndPersistChanges();

        StatusChange ch = changes.stream()
                .filter(c -> c.login().equals("lucankri"))
                .findFirst().orElse(null);

        assertNotNull(ch, "Должно быть изменение для lucankri");
        assertTrue(ch.newStatus(), "Должен быть переход в ONLINE");
        assertEquals(Set.of("703226616"), new HashSet<>(ch.telegramIds()));

        Online after = onlineRepository.findByLogin("lucankri").orElseThrow();
        assertTrue(after.getIsOnline(), "Статус lucankri должен стать TRUE");
        assertNull(after.getLastSeenAt(), "Для ONLINE-перехода lastSeenAt не должен выставляться");
    }

    @Test
    void computeAndPersistChanges_noChange_for_elevante() {
        // pre: elevante и так в кампусе и уже online=true (по фикстурам online_import.sql)
        Online before = onlineRepository.findByLogin("elevante").orElseThrow();
        assertTrue(before.getIsOnline());

        List<StatusChange> changes = notifyService.computeAndPersistChanges();

        boolean hasElevante = changes.stream().anyMatch(c -> c.login().equals("elevante"));
        assertFalse(hasElevante, "Изменений для elevante быть не должно");

        Online after = onlineRepository.findByLogin("elevante").orElseThrow();
        assertTrue(after.getIsOnline());
    }

    @Test
    void computeAndPersistChanges_notInCampus_for_scrimgew_noChange_andRemainsFalse() {
        // scrimgew подписан, но не в workplace → запись создана @BeforeEach со значением false
        Online online = onlineRepository.findByLogin("scrimgew").orElseThrow();
        assertFalse(online.getIsOnline(), "scrimgew должен быть FALSE до прогона");

        List<StatusChange> changes = notifyService.computeAndPersistChanges();

        boolean hasScrimgew = changes.stream().anyMatch(c -> c.login().equals("scrimgew"));
        assertFalse(hasScrimgew, "Изменений для scrimgew быть не должно");

        Online after = onlineRepository.findByLogin("scrimgew").orElseThrow();
        assertFalse(after.getIsOnline(), "scrimgew остаётся FALSE и запись не удаляется");
    }

    @Test
    void computeAndPersistChanges_offline_for_elevante_updatesDb_andReturnsChange() {
        // pre: elevante online=true и в кампусе
        Online before = onlineRepository.findByLogin("elevante").orElseThrow();
        assertTrue(before.getIsOnline());

        // имитируем уход из кампуса
        workplaceRepository.deleteAll(
                workplaceRepository.findAll().stream()
                        .filter(w -> Objects.equals(w.getLogin(), "elevante"))
                        .toList()
        );

        List<StatusChange> changes = notifyService.computeAndPersistChanges();

        StatusChange ch = changes.stream()
                .filter(c -> c.login().equals("elevante"))
                .findFirst().orElse(null);

        assertNotNull(ch, "Должно быть изменение для elevante (OFFLINE)");
        assertFalse(ch.newStatus(), "Переход в OFFLINE");
        assertEquals(Set.of("70735394", "703226616"), new HashSet<>(ch.telegramIds()));

        Online after = onlineRepository.findByLogin("elevante").orElseThrow();
        assertFalse(after.getIsOnline(), "Статус elevante должен стать FALSE");
        assertNotNull(after.getLastSeenAt(), "Для OFFLINE-перехода должен выставляться lastSeenAt");
    }
}
