package ru.izpz.edu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import ru.izpz.dto.FriendDto;
import ru.izpz.dto.FriendRequest;
import ru.izpz.dto.FriendsSliceDto;
import ru.izpz.edu.mapper.FriendsMapper;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Friends;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.repository.FriendsRepository;
import ru.izpz.edu.repository.ParticipantRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendsRepository friendsRepository;
    
    @Mock
    private FriendsMapper friendsMapper;
    
    @Mock
    private WorkplaceService workplaceService;
    
    @Mock
    private ClusterRepository clusterRepository;
    
    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private FriendService friendService;

    private Friends testFriend;
    private FriendDto testFriendDto;

    @BeforeEach
    void setUp() {
        testFriend = new Friends();
        testFriend.setId(java.util.UUID.randomUUID());
        testFriend.setTelegramId("123456");
        testFriend.setLogin("testuser");
        testFriend.setIsFriend(false);
        testFriend.setIsFavorite(false);
        testFriend.setIsSubscribe(false);
        testFriend.setName("");
        testFriend.setDate(LocalDateTime.now());

        testFriendDto = FriendDto.builder()
                .telegramId(testFriend.getTelegramId())
                .login(testFriend.getLogin())
                .isFriend(testFriend.getIsFriend())
                .isFavorite(testFriend.getIsFavorite())
                .isSubscribe(testFriend.getIsSubscribe())
                .name(testFriend.getName())
                .build();

        lenient().when(friendsRepository.save(any(Friends.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(friendsMapper.toDto(any(Friends.class))).thenAnswer(inv -> {
            Friends f = inv.getArgument(0);
            return FriendDto.builder()
                    .telegramId(f.getTelegramId())
                    .login(f.getLogin())
                    .isFriend(f.getIsFriend())
                    .isFavorite(f.getIsFavorite())
                    .isSubscribe(f.getIsSubscribe())
                    .name(f.getName())
                    .build();
        });
    }

    @Test
    void applyFriend_shouldCreateNewFriend_whenNotFound() {
        // Given
        String telegramId = "123456";
        String login = "newuser";
        FriendRequest.Action action = FriendRequest.Action.TOGGLE_FRIEND;
        
        when(friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login))
                .thenReturn(Optional.empty());

        // When
        FriendDto result = friendService.applyFriend(telegramId, login, action, null);

        // Then
        assertNotNull(result);
        verify(friendsRepository, times(2)).save(any(Friends.class));
        verify(friendsMapper).toDto(any(Friends.class));
    }

    @Test
    void applyFriend_shouldToggleFriend_whenActionIsToggleFriend() {
        // Given
        String telegramId = "123456";
        String login = "testuser";
        FriendRequest.Action action = FriendRequest.Action.TOGGLE_FRIEND;
        
        testFriend.setIsFriend(false);
        when(friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login))
                .thenReturn(Optional.of(testFriend));

        // When
        FriendDto result = friendService.applyFriend(telegramId, login, action, null);

        // Then
        assertTrue(result.getIsFriend());
        verify(friendsRepository).save(testFriend);
    }

    @Test
    void applyFriend_shouldResetFlags_whenFriendIsSetToFalse() {
        // Given
        String telegramId = "123456";
        String login = "testuser";
        FriendRequest.Action action = FriendRequest.Action.TOGGLE_FRIEND;
        
        testFriend.setIsFriend(true);
        testFriend.setIsFavorite(true);
        testFriend.setIsSubscribe(true);
        testFriend.setName("Test Name");
        
        when(friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login))
                .thenReturn(Optional.of(testFriend));

        // When
        FriendDto result = friendService.applyFriend(telegramId, login, action, null);

        // Then
        assertFalse(result.getIsFriend());
        assertFalse(result.getIsFavorite());
        assertFalse(result.getIsSubscribe());
        assertEquals("", result.getName());
        verify(friendsRepository).save(testFriend);
    }

    @Test
    void applyFriend_shouldToggleFavorite_whenActionIsToggleFavorite() {
        // Given
        String telegramId = "123456";
        String login = "testuser";
        FriendRequest.Action action = FriendRequest.Action.TOGGLE_FAVORITE;
        
        testFriend.setIsFavorite(false);
        when(friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login))
                .thenReturn(Optional.of(testFriend));

        // When
        FriendDto result = friendService.applyFriend(telegramId, login, action, null);

        // Then
        assertTrue(result.getIsFavorite());
        verify(friendsRepository).save(testFriend);
    }

    @Test
    void applyFriend_shouldToggleSubscribe_whenActionIsToggleSubscribe() {
        // Given
        String telegramId = "123456";
        String login = "testuser";
        FriendRequest.Action action = FriendRequest.Action.TOGGLE_SUBSCRIBE;
        
        testFriend.setIsSubscribe(false);
        when(friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login))
                .thenReturn(Optional.of(testFriend));

        // When
        FriendDto result = friendService.applyFriend(telegramId, login, action, null);

        // Then
        assertTrue(result.getIsSubscribe());
        verify(friendsRepository).save(testFriend);
    }

    @Test
    void applyFriend_shouldSetName_whenActionIsSetName() {
        // Given
        String telegramId = "123456";
        String login = "testuser";
        String name = "New Name";
        FriendRequest.Action action = FriendRequest.Action.SET_NAME;
        
        when(friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login))
                .thenReturn(Optional.of(testFriend));

        // When
        FriendDto result = friendService.applyFriend(telegramId, login, action, name);

        // Then
        assertEquals(name, result.getName());
        verify(friendsRepository).save(testFriend);
    }

    @Test
    void applyFriend_shouldTrimName_whenActionIsSetName() {
        // Given
        String telegramId = "123456";
        String login = "testuser";
        String name = "  New Name  ";
        FriendRequest.Action action = FriendRequest.Action.SET_NAME;
        
        when(friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login))
                .thenReturn(Optional.of(testFriend));

        // When
        FriendDto result = friendService.applyFriend(telegramId, login, action, name);

        // Then
        assertEquals("New Name", result.getName());
        verify(friendsRepository).save(testFriend);
    }

    @Test
    void applyFriend_shouldReturnExistingFriend_whenActionIsNone() {
        // Given
        String telegramId = "123456";
        String login = "testuser";
        FriendRequest.Action action = FriendRequest.Action.NONE;
        
        when(friendsRepository.findFirstByTelegramIdAndLogin(telegramId, login))
                .thenReturn(Optional.of(testFriend));

        // When
        FriendDto result = friendService.applyFriend(telegramId, login, action, null);

        // Then
        assertEquals(testFriend.getTelegramId(), result.getTelegramId());
        assertEquals(testFriend.getLogin(), result.getLogin());
        assertEquals(testFriend.getName(), result.getName());
        verify(friendsRepository, never()).save(any());
    }

    @Test
    void getFriends_shouldReturnFriendsSliceDto() {
        // Given
        String telegramId = "123456";
        int page = 0;
        int size = 10;
        
        List<Friends> friendsList = List.of(testFriend);
        Slice<Friends> slice = new SliceImpl<>(friendsList, PageRequest.of(page, size), false);
        
        Workplace workplace = new Workplace();
        workplace.setLogin("testuser");
        workplace.setId(new WorkplaceId(1L, "A", 101));
        
        Cluster cluster = new Cluster();
        cluster.setClusterId(1L);
        cluster.setName("Test Cluster");
        
        when(friendsRepository.findAllOrdered(telegramId, PageRequest.of(page, size)))
                .thenReturn(slice);
        when(workplaceService.findAllByLoginIn(List.of("testuser")))
                .thenReturn(List.of(workplace));
        when(clusterRepository.findAllByClusterIdIn(Set.of(1L)))
                .thenReturn(List.of(cluster));
        when(participantRepository.findAllViewByLoginIn(List.of("testuser")))
                .thenReturn(List.of());

        // When
        FriendsSliceDto result = friendService.getFriends(telegramId, page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertFalse(result.hasNext());
        verify(friendsRepository).findAllOrdered(telegramId, PageRequest.of(page, size));
        verify(workplaceService).findAllByLoginIn(List.of("testuser"));
        verify(clusterRepository).findAllByClusterIdIn(Set.of(1L));
        verify(participantRepository).findAllViewByLoginIn(List.of("testuser"));
    }

    @Test
    void getFriends_shouldHandleEmptyFriendsList() {
        // Given
        String telegramId = "123456";
        int page = 0;
        int size = 10;
        
        Slice<Friends> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(page, size), false);
        
        when(friendsRepository.findAllOrdered(telegramId, PageRequest.of(page, size)))
                .thenReturn(emptySlice);

        // When
        FriendsSliceDto result = friendService.getFriends(telegramId, page, size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.content().size());
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertFalse(result.hasNext());
        verify(friendsRepository).findAllOrdered(telegramId, PageRequest.of(page, size));
        verify(workplaceService).findAllByLoginIn(List.of());
        verify(clusterRepository).findAllByClusterIdIn(Set.of());
        verify(participantRepository).findAllViewByLoginIn(List.of());
    }
}
