package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.model.Online;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.repository.FriendsRepository;
import ru.izpz.edu.repository.OnlineRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NotifyServiceUnitTest {

    @Test
    void computeAndPersistChanges_returnsEmpty_whenNoTrackedLogins() {
        FriendsRepository friendsRepository = mock(FriendsRepository.class);
        WorkplaceRepository workplaceRepository = mock(WorkplaceRepository.class);
        OnlineRepository onlineRepository = mock(OnlineRepository.class);
        NotifyService service = new NotifyService(friendsRepository, workplaceRepository, onlineRepository);

        when(friendsRepository.findDistinctLogins()).thenReturn(List.of());

        List<StatusChange> result = service.computeAndPersistChanges();

        assertTrue(result.isEmpty());
        verifyNoInteractions(workplaceRepository, onlineRepository);
    }

    @Test
    void computeAndPersistChanges_doesNotEmitChange_whenNoSubscribers() {
        FriendsRepository friendsRepository = mock(FriendsRepository.class);
        WorkplaceRepository workplaceRepository = mock(WorkplaceRepository.class);
        OnlineRepository onlineRepository = mock(OnlineRepository.class);
        NotifyService service = new NotifyService(friendsRepository, workplaceRepository, onlineRepository);

        when(friendsRepository.findDistinctLogins()).thenReturn(List.of("alice"));
        Workplace workplace = new Workplace();
        workplace.setLogin("alice");
        workplace.setId(new WorkplaceId(1L, "A", 1));
        when(workplaceRepository.findAllByLoginIn(List.of("alice"))).thenReturn(List.of(workplace));
        when(onlineRepository.findByLogin("alice")).thenReturn(Optional.empty());
        when(friendsRepository.findByLoginAndIsSubscribeTrue("alice")).thenReturn(List.of());

        List<StatusChange> result = service.computeAndPersistChanges();

        assertTrue(result.isEmpty());
        verify(onlineRepository).save(any(Online.class));
    }

    @Test
    void computeAndPersistChanges_doesNotUpdate_whenOfflineRecordAlreadyFalse() {
        FriendsRepository friendsRepository = mock(FriendsRepository.class);
        WorkplaceRepository workplaceRepository = mock(WorkplaceRepository.class);
        OnlineRepository onlineRepository = mock(OnlineRepository.class);
        NotifyService service = new NotifyService(friendsRepository, workplaceRepository, onlineRepository);

        Online existing = new Online();
        existing.setLogin("bob");
        existing.setIsOnline(false);

        when(friendsRepository.findDistinctLogins()).thenReturn(List.of("bob"));
        when(workplaceRepository.findAllByLoginIn(List.of("bob"))).thenReturn(List.of());
        when(onlineRepository.findByLogin("bob")).thenReturn(Optional.of(existing));

        List<StatusChange> result = service.computeAndPersistChanges();

        assertTrue(result.isEmpty());
        verify(onlineRepository, never()).save(any(Online.class));
    }

    @Test
    void computeAndPersistChanges_doesNothing_whenOfflineAndNoOnlineRecord() {
        FriendsRepository friendsRepository = mock(FriendsRepository.class);
        WorkplaceRepository workplaceRepository = mock(WorkplaceRepository.class);
        OnlineRepository onlineRepository = mock(OnlineRepository.class);
        NotifyService service = new NotifyService(friendsRepository, workplaceRepository, onlineRepository);

        when(friendsRepository.findDistinctLogins()).thenReturn(List.of("eve"));
        when(workplaceRepository.findAllByLoginIn(List.of("eve"))).thenReturn(List.of());
        when(onlineRepository.findByLogin("eve")).thenReturn(Optional.empty());

        List<StatusChange> result = service.computeAndPersistChanges();

        assertTrue(result.isEmpty());
        verify(onlineRepository, never()).save(any(Online.class));
    }

    @Test
    void computeAndPersistChanges_setsLastSeenAt_whenTransitionToOffline() {
        FriendsRepository friendsRepository = mock(FriendsRepository.class);
        WorkplaceRepository workplaceRepository = mock(WorkplaceRepository.class);
        OnlineRepository onlineRepository = mock(OnlineRepository.class);
        NotifyService service = new NotifyService(friendsRepository, workplaceRepository, onlineRepository);

        Online existing = new Online();
        existing.setLogin("carol");
        existing.setIsOnline(true);

        when(friendsRepository.findDistinctLogins()).thenReturn(List.of("carol"));
        when(workplaceRepository.findAllByLoginIn(List.of("carol"))).thenReturn(List.of());
        when(onlineRepository.findByLogin("carol")).thenReturn(Optional.of(existing));
        when(friendsRepository.findByLoginAndIsSubscribeTrue("carol")).thenReturn(List.of());

        List<StatusChange> result = service.computeAndPersistChanges();

        assertTrue(result.isEmpty());
        verify(onlineRepository).save(argThat(saved ->
                Boolean.FALSE.equals(saved.getIsOnline()) && saved.getLastSeenAt() != null
        ));
    }
}
