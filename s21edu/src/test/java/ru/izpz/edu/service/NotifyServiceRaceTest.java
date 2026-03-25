package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.model.Friends;
import ru.izpz.edu.model.Online;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.repository.FriendsRepository;
import ru.izpz.edu.repository.OnlineRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyServiceRaceTest {

    @Mock
    private FriendsRepository friendsRepository;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private OnlineRepository onlineRepository;

    @InjectMocks
    private NotifyService notifyService;

    @Test
    void computeAndPersistChanges_shouldHandleDuplicateOnOnlineInsertWithoutThrowing() {
        String login = "john";
        Friends friend = new Friends();
        friend.setTelegramId("111");
        friend.setLogin(login);
        Online existing = new Online();
        existing.setLogin(login);
        existing.setIsOnline(false);

        when(friendsRepository.findDistinctLogins()).thenReturn(List.of(login));
        Workplace workplace = new Workplace();
        workplace.setLogin(login);
        workplace.setId(new WorkplaceId(1L, "A", 1));
        when(workplaceRepository.findAllByLoginIn(List.of(login))).thenReturn(List.of(workplace));
        when(onlineRepository.findByLogin(login)).thenReturn(Optional.empty(), Optional.of(existing));
        when(onlineRepository.save(any(Online.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"))
            .thenReturn(existing);
        when(friendsRepository.findByLoginAndIsSubscribeTrue(login)).thenReturn(List.of(friend));

        List<StatusChange> changes = notifyService.computeAndPersistChanges();

        assertNotNull(changes);
        assertEquals(1, changes.size());
        assertEquals(login, changes.getFirst().login());
        assertEquals(List.of("111"), changes.getFirst().telegramIds());
        assertFalse(changes.isEmpty());
        verify(onlineRepository, times(2)).findByLogin(login);
        verify(onlineRepository, times(2)).save(any(Online.class));
    }

    @Test
    void computeAndPersistChanges_shouldHandleDuplicateOnOfflineUpdateAndSetLastSeen() {
        String login = "john";
        Friends friend = new Friends();
        friend.setTelegramId("111");
        friend.setLogin(login);
        Online existing = new Online();
        existing.setLogin(login);
        existing.setIsOnline(true);

        when(friendsRepository.findDistinctLogins()).thenReturn(List.of(login));
        when(workplaceRepository.findAllByLoginIn(List.of(login))).thenReturn(List.of());
        when(onlineRepository.findByLogin(login)).thenReturn(Optional.of(existing), Optional.of(existing));
        when(onlineRepository.save(any(Online.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"))
            .thenReturn(existing);
        when(friendsRepository.findByLoginAndIsSubscribeTrue(login)).thenReturn(List.of(friend));

        List<StatusChange> changes = notifyService.computeAndPersistChanges();

        assertNotNull(changes);
        assertEquals(1, changes.size());
        assertEquals(login, changes.getFirst().login());
        assertTrue(changes.getFirst().telegramIds().contains("111"));
        verify(onlineRepository, times(2)).save(any(Online.class));
    }
}
