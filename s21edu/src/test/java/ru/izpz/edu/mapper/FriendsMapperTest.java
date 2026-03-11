package ru.izpz.edu.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.izpz.dto.FriendDto;
import ru.izpz.edu.model.Friends;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FriendsMapperTest {

    private final FriendsMapper friendsMapper = Mappers.getMapper(FriendsMapper.class);

    @Test
    void toDto_shouldMapAllFields() {
        // Arrange
        Friends friend = new Friends();
        UUID id = UUID.randomUUID();
        friend.setId(id);
        friend.setTelegramId("123456");
        friend.setLogin("testuser");
        friend.setName("Test User");
        friend.setIsFriend(true);
        friend.setIsFavorite(false);
        friend.setIsSubscribe(true);
        friend.setDate(LocalDateTime.of(2023, 1, 1, 12, 0, 0));

        // Act
        FriendDto result = friendsMapper.toDto(friend);

        // Assert
        assertNotNull(result);
        assertEquals("123456", result.getTelegramId());
        assertEquals("testuser", result.getLogin());
        assertEquals("Test User", result.getName());
        assertTrue(result.getIsFriend());
        assertFalse(result.getIsFavorite());
        assertTrue(result.getIsSubscribe());
    }

    @Test
    void toDto_shouldHandleNullValues() {
        // Arrange
        Friends friend = new Friends();
        friend.setTelegramId(null);
        friend.setLogin(null);
        friend.setName(null);
        friend.setIsFriend(null);
        friend.setIsFavorite(null);
        friend.setIsSubscribe(null);
        friend.setDate(null);

        // Act
        FriendDto result = friendsMapper.toDto(friend);

        // Assert
        assertNotNull(result);
        assertNull(result.getTelegramId());
        assertNull(result.getLogin());
        assertNull(result.getName());
        assertNull(result.getIsFriend());
        assertNull(result.getIsFavorite());
        assertNull(result.getIsSubscribe());
    }

    @Test
    void toDtos_shouldMapList() {
        // Arrange
        Friends friend1 = new Friends();
        friend1.setId(UUID.randomUUID());
        friend1.setLogin("user1");
        
        Friends friend2 = new Friends();
        friend2.setId(UUID.randomUUID());
        friend2.setLogin("user2");
        
        List<Friends> friends = List.of(friend1, friend2);

        // Act
        List<FriendDto> result = friendsMapper.toDtos(friends);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getLogin());
        assertEquals("user2", result.get(1).getLogin());
    }

    @Test
    void toDtos_shouldHandleEmptyList() {
        // Arrange
        List<Friends> emptyFriends = List.of();

        // Act
        List<FriendDto> result = friendsMapper.toDtos(emptyFriends);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toDtos_shouldHandleNullList() {
        // Arrange
        List<Friends> nullFriends = null;

        // Act
        List<FriendDto> result = friendsMapper.toDtos(nullFriends);

        // Assert
        assertNull(result);
    }
}
