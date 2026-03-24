package ru.izpz.edu.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.izpz.dto.LastCommandState;
import ru.izpz.dto.LastCommandType;
import ru.izpz.dto.ParticipantStatusEnum;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.model.ParticipantCampusV1DTO;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.model.Profile;

import java.util.Map;
import java.util.UUID;

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

    @Test
    void toDto_shouldMapParticipantFromApiDto() {
        ParticipantCampusV1DTO campus = new ParticipantCampusV1DTO();
        UUID campusId = UUID.randomUUID();
        campus.setId(campusId);
        campus.setShortName("KZN");

        ParticipantV1DTO dto = new ParticipantV1DTO();
        dto.setLogin("testuser");
        dto.setClassName("B17");
        dto.setParallelName("java");
        dto.setExpValue(1200L);
        dto.setLevel(5);
        dto.setExpToNextLevel(300L);
        dto.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        dto.setCampus(campus);

        var result = profileMapper.toDto(dto);

        assertNotNull(result);
        assertEquals("testuser", result.getLogin());
        assertEquals("B17", result.getClassName());
        assertEquals("java", result.getParallelName());
        assertEquals(1200, result.getExpValue());
        assertEquals(5, result.getLevel());
        assertEquals(300, result.getExpToNextLevel());
        assertEquals(ParticipantStatusEnum.ACTIVE, result.getStatus());
        assertNotNull(result.getCampus());
        assertEquals(campusId.toString(), result.getCampus().getId());
        assertEquals("KZN", result.getCampus().getCampusName());
    }

    @Test
    void toDto_shouldReturnNullForNullParticipantApiDto() {
        assertNull(profileMapper.toDto((ParticipantV1DTO) null));
    }

    @Test
    void toDto_shouldMapCampusFromApiDto() {
        ParticipantCampusV1DTO campus = new ParticipantCampusV1DTO();
        UUID campusId = UUID.randomUUID();
        campus.setId(campusId);
        campus.setShortName("MSK");

        var result = profileMapper.toDto(campus);

        assertNotNull(result);
        assertEquals(campusId.toString(), result.getId());
        assertEquals("MSK", result.getCampusName());
    }

    @Test
    void toDto_shouldReturnNullForNullCampusApiDto() {
        assertNull(profileMapper.toDto((ParticipantCampusV1DTO) null));
    }
}
