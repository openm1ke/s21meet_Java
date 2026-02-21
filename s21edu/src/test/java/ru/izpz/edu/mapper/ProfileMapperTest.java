package ru.izpz.edu.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.izpz.dto.LastCommandState;
import ru.izpz.dto.LastCommandType;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.edu.model.Profile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfileMapperTest {

    private final ProfileMapper profileMapper = Mappers.getMapper(ProfileMapper.class);

    @Test
    void toDto_shouldMapAllFields() {
        // Arrange
        Profile profile = new Profile();
        profile.setId(java.util.UUID.randomUUID());
        profile.setTelegramId("123456");
        profile.setS21login("testuser");
        profile.setStatus(ProfileStatus.CONFIRMED);
        LastCommandState lastCommand = new LastCommandState(LastCommandType.SEARCH, Map.of("param1", "value1"));
        profile.setLastCommand(lastCommand);

        // Act
        ProfileDto result = profileMapper.toDto(profile);

        // Assert
        assertNotNull(result);
        assertEquals("123456", result.telegramId());
        assertEquals("testuser", result.s21login());
        assertEquals(ProfileStatus.CONFIRMED, result.status());
        assertEquals(lastCommand, result.lastCommand());
    }

    @Test
    void toDto_shouldHandleNullValues() {
        // Arrange
        Profile profile = new Profile();
        profile.setTelegramId(null);
        profile.setS21login(null);
        profile.setStatus(null);
        profile.setLastCommand(null);

        // Act
        ProfileDto result = profileMapper.toDto(profile);

        // Assert
        assertNotNull(result);
        assertNull(result.telegramId());
        assertNull(result.s21login());
        assertNull(result.status());
        assertNull(result.lastCommand());
    }

    @Test
    void toEntity_shouldMapAllFields() {
        // Arrange
        ProfileDto dto = new ProfileDto(
                "123456",
                "testuser", 
                ProfileStatus.CONFIRMED,
                new LastCommandState(LastCommandType.SET_NAME, Map.of("name", "test"))
        );

        // Act
        Profile result = profileMapper.toEntity(dto);

        // Assert
        assertNotNull(result);
        assertEquals("123456", result.getTelegramId());
        assertEquals("testuser", result.getS21login());
        assertEquals(ProfileStatus.CONFIRMED, result.getStatus());
        assertEquals(dto.lastCommand(), result.getLastCommand());
    }

    @Test
    void toEntity_shouldHandleNullDto() {
        // Arrange
        ProfileDto nullDto = null;

        // Act
        Profile result = profileMapper.toEntity(nullDto);

        // Assert
        assertNull(result);
    }
}
