package ru.izpz.edu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.*;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ParticipantCampusV1DTO;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.exception.EntityNotFoundException;
import ru.izpz.edu.mapper.ProfileMapper;
import ru.izpz.edu.mapper.ProfileVerificationMapper;
import ru.izpz.edu.model.Participant;
import ru.izpz.edu.model.ParticipantCampus;
import ru.izpz.edu.model.Profile;
import ru.izpz.edu.model.ProfileValidation;
import ru.izpz.edu.model.StudentCoalition;
import ru.izpz.edu.repository.ParticipantCampusRepository;
import ru.izpz.edu.repository.ParticipantRepository;
import ru.izpz.edu.repository.ProfileRepository;
import ru.izpz.edu.repository.ProfileValidationRepository;
import ru.izpz.edu.repository.StudentCoalitionRepository;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileMapper profileMapper;
    
    @Mock
    private ProfileVerificationMapper profileVerificationMapper;
    
    @Mock
    private ProfileRepository profileRepository;
    
    @Mock
    private ProfileValidationRepository profileValidationRepository;
    
    @Mock
    private ParticipantApi participantApi;
    
    @Mock
    private ParticipantRepository participantRepository;
    
    @Mock
    private ParticipantCampusRepository participantCampusRepository;
    
    @Mock
    private CampusCatalog campusCatalog;
    @Mock
    private StudentCoalitionRepository studentCoalitionRepository;
    @Mock
    private GraphQLService graphQLService;

    @InjectMocks
    private ProfileService profileService;

    private Profile testProfile;
    private ProfileDto testProfileDto;

    @BeforeEach
    void setUp() {
        testProfile = new Profile();
        testProfile.setId(java.util.UUID.randomUUID());
        testProfile.setTelegramId("123456");
        testProfile.setS21login("testuser");
        testProfile.setStatus(ProfileStatus.CREATED);

        testProfileDto = new ProfileDto(
                testProfile.getTelegramId(),
                testProfile.getS21login(),
                testProfile.getStatus(),
                null
        );

        lenient().when(profileMapper.toDto(any(Profile.class))).thenAnswer(inv -> {
            Profile p = inv.getArgument(0);
            return new ProfileDto(p.getTelegramId(), p.getS21login(), p.getStatus(), p.getLastCommand());
        });
        lenient().when(campusCatalog.targetCampusIds()).thenReturn(java.util.List.of(
                "6bfe3c56-0211-4fe1-9e59-51616caac4dd",
                "7c293c9c-f28c-4b10-be29-560e4b000a34"
        ));
        lenient().when(campusCatalog.campusName("6bfe3c56-0211-4fe1-9e59-51616caac4dd")).thenReturn("MSK");
        lenient().when(campusCatalog.campusName("7c293c9c-f28c-4b10-be29-560e4b000a34")).thenReturn("KZN");
        lenient().when(participantRepository.findByLogin(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void getOrCreateProfile_shouldReturnExistingProfile_whenFound() {
        // Given
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.of(testProfile));
        when(profileMapper.toDto(testProfile)).thenReturn(testProfileDto);

        // When
        ProfileDto result = profileService.getOrCreateProfile("123456");

        // Then
        assertNotNull(result);
        assertEquals(testProfileDto, result);
        verify(profileRepository).findByTelegramId("123456");
        verify(graphQLService).refreshStudentCoalitionByLogin("testuser");
        verify(profileRepository, never()).save(any());
    }

    @Test
    void getOrCreateProfile_shouldCreateNewProfile_whenNotFound() {
        // Given
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
        when(profileMapper.toDto(testProfile)).thenReturn(testProfileDto);

        // When
        ProfileDto result = profileService.getOrCreateProfile("123456");

        // Then
        assertNotNull(result);
        assertEquals(testProfileDto, result);
        verify(profileRepository).findByTelegramId("123456");
        verify(graphQLService).refreshStudentCoalitionByLogin("testuser");
        verify(profileRepository).save(any(Profile.class));
        assertEquals(ProfileStatus.CREATED, testProfile.getStatus());
    }

    @Test
    void getOrCreateProfile_shouldNotRefreshCoalition_whenLoginIsMissing() {
        Profile profile = new Profile();
        profile.setTelegramId("123456");
        profile.setS21login(null);
        profile.setStatus(ProfileStatus.CREATED);
        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(profile));

        ProfileDto result = profileService.getOrCreateProfile("123456");

        assertNotNull(result);
        verify(graphQLService, never()).refreshStudentCoalitionByLogin(anyString());
    }

    @Test
    void getOrCreateProfile_shouldFallbackToRead_whenSaveRaceConditionHappens() {
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.empty(), Optional.of(testProfile));
        when(profileRepository.save(any(Profile.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(profileMapper.toDto(testProfile)).thenReturn(testProfileDto);

        ProfileDto result = profileService.getOrCreateProfile("123456");

        assertNotNull(result);
        assertEquals(testProfileDto, result);
        verify(profileRepository, times(2)).findByTelegramId("123456");
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void getOrCreateProfile_shouldFallbackToInMemoryProfile_whenSaveRaceConditionAndReloadMissing() {
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.empty(), Optional.empty());
        when(profileRepository.save(any(Profile.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        ProfileDto result = profileService.getOrCreateProfile("123456");

        assertNotNull(result);
        assertEquals("123456", result.telegramId());
        assertEquals(ProfileStatus.CREATED, result.status());
        verify(profileRepository, times(2)).findByTelegramId("123456");
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void getProfile_shouldReturnProfile_whenFound() {
        // Given
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.of(testProfile));
        when(profileMapper.toDto(testProfile)).thenReturn(testProfileDto);

        // When
        ProfileDto result = profileService.getProfile("123456");

        // Then
        assertNotNull(result);
        assertEquals(testProfileDto, result);
        verify(profileRepository).findByTelegramId("123456");
    }

    @Test
    void getProfile_shouldThrowException_whenNotFound() {
        // Given
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> profileService.getProfile("123456")
        );
        assertTrue(exception.getMessage().contains("Профиль не найден для telegramId = 123456"));
        verify(profileRepository).findByTelegramId("123456");
    }

    @Test
    void updateProfileStatus_shouldUpdateStatus_whenProfileExists() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .telegramId("123456")
                .status(ProfileStatus.CONFIRMED)
                .build();
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.of(testProfile));
        when(profileRepository.save(testProfile)).thenReturn(testProfile);

        // When
        ProfileDto result = profileService.updateProfileStatus(request);

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.CONFIRMED, result.status());
        verify(profileRepository).findByTelegramId("123456");
        verify(profileRepository).save(testProfile);
    }

    @Test
    void updateProfileStatus_shouldThrowException_whenProfileNotFound() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .telegramId("123456")
                .status(ProfileStatus.CONFIRMED)
                .build();
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> profileService.updateProfileStatus(request)
        );
        assertTrue(exception.getMessage().contains("Профиль не найден для telegramId = 123456"));
        verify(profileRepository).findByTelegramId("123456");
        verify(profileRepository, never()).save(any());
    }

    @Test
    void checkAndSetLogin_shouldSetLogin_whenValid() {
        // Given
        when(profileRepository.existsByS21login("newuser")).thenReturn(false);
        testProfile.setS21login(null);
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.of(testProfile));
        when(profileRepository.save(testProfile)).thenReturn(testProfile);

        // When
        ProfileDto result = profileService.checkAndSetLogin("123456", "newuser");

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.s21login());
        verify(profileRepository).existsByS21login("newuser");
        verify(profileRepository).findByTelegramId("123456");
        verify(profileRepository).save(testProfile);
    }

    @Test
    void checkAndSetLogin_shouldFallbackToRead_whenSaveRaceConditionHappens() {
        testProfile.setS21login(null);
        Profile savedByAnotherTx = new Profile();
        savedByAnotherTx.setTelegramId("123456");
        savedByAnotherTx.setS21login("newuser");
        savedByAnotherTx.setStatus(ProfileStatus.CREATED);

        ProfileDto expected = new ProfileDto("123456", "newuser", ProfileStatus.CREATED, null);

        when(profileRepository.existsByS21login("newuser")).thenReturn(false);
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.of(testProfile), Optional.of(savedByAnotherTx));
        when(profileRepository.save(testProfile))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(profileMapper.toDto(savedByAnotherTx)).thenReturn(expected);

        ProfileDto result = profileService.checkAndSetLogin("123456", "newuser");

        assertNotNull(result);
        assertEquals("newuser", result.s21login());
        verify(profileRepository).save(testProfile);
        verify(profileRepository, times(2)).findByTelegramId("123456");
    }

    @Test
    void checkAndSetLogin_shouldFallbackToInMemoryProfile_whenSaveRaceConditionAndReloadMissing() {
        testProfile.setS21login(null);

        when(profileRepository.existsByS21login("newuser")).thenReturn(false);
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.of(testProfile), Optional.empty());
        when(profileRepository.save(testProfile))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        ProfileDto result = profileService.checkAndSetLogin("123456", "newuser");

        assertNotNull(result);
        assertEquals("newuser", result.s21login());
        verify(profileRepository).save(testProfile);
        verify(profileRepository, times(2)).findByTelegramId("123456");
    }

    @Test
    void checkAndSetLogin_shouldThrowException_whenLoginAlreadyExists() {
        // Given
        when(profileRepository.existsByS21login("existinguser")).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> profileService.checkAndSetLogin("123456", "existinguser")
        );
        assertTrue(exception.getMessage().contains("Логин existinguser уже привязан к другому профилю"));
        verify(profileRepository).existsByS21login("existinguser");
        verify(profileRepository, never()).findByTelegramId(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void checkAndSetLogin_shouldThrowException_whenProfileAlreadyHasLogin() {
        // Given
        when(profileRepository.existsByS21login("anotheruser")).thenReturn(false);
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.of(testProfile));

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> profileService.checkAndSetLogin("123456", "anotheruser")
        );
        assertTrue(exception.getMessage().contains("Профиль уже привязан к логину testuser"));
        verify(profileRepository).existsByS21login("anotheruser");
        verify(profileRepository).findByTelegramId("123456");
        verify(profileRepository, never()).save(any());
    }

    @Test
    void checkAndSetLogin_shouldThrowException_whenProfileNotFound() {
        // Given
        when(profileRepository.existsByS21login("newuser")).thenReturn(false);
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> profileService.checkAndSetLogin("123456", "newuser")
        );
        assertTrue(exception.getMessage().contains("Профиль не найден для telegramId = 123456"));
        verify(profileRepository).existsByS21login("newuser");
        verify(profileRepository).findByTelegramId("123456");
        verify(profileRepository, never()).save(any());
    }

    @Test
    void getVerificationCode_shouldReturnExistingCode_whenExists() {
        // Given
        ProfileValidation validation = new ProfileValidation();
        validation.setS21login("testuser");
        validation.setSecretCode("1234");
        validation.setExpiresAt(OffsetDateTime.now());
        
        ProfileCodeResponse expectedResponse = new ProfileCodeResponse("testuser", "1234", validation.getExpiresAt());
        
        when(profileValidationRepository.findByS21login("testuser"))
                .thenReturn(Optional.of(validation));
        when(profileVerificationMapper.toProfileCodeResponse(validation))
                .thenReturn(expectedResponse);

        // When
        ProfileCodeResponse result = profileService.getVerificationCode("testuser");

        // Then
        assertNotNull(result);
        assertEquals("1234", result.getSecretCode());
        verify(profileValidationRepository).findByS21login("testuser");
        verify(profileValidationRepository, never()).save(any());
    }

    @Test
    void getVerificationCode_shouldCreateNewCode_whenNotExists() {
        // Given
        ProfileValidation validation = new ProfileValidation();
        validation.setS21login("testuser");
        validation.setSecretCode("5678");
        validation.setExpiresAt(OffsetDateTime.now());
        
        ProfileCodeResponse expectedResponse = new ProfileCodeResponse("testuser", "5678", validation.getExpiresAt());
        
        when(profileValidationRepository.findByS21login("testuser"))
                .thenReturn(Optional.empty());
        when(profileValidationRepository.save(any(ProfileValidation.class)))
                .thenReturn(validation);
        when(profileVerificationMapper.toProfileCodeResponse(validation))
                .thenReturn(expectedResponse);

        // When
        ProfileCodeResponse result = profileService.getVerificationCode("testuser");

        // Then
        assertNotNull(result);
        assertEquals("5678", result.getSecretCode());
        verify(profileValidationRepository).findByS21login("testuser");
        verify(profileValidationRepository).save(any(ProfileValidation.class));
    }

    @Test
    void getVerificationCode_shouldFallbackToRead_whenSaveRaceConditionHappens() {
        ProfileValidation existing = new ProfileValidation();
        existing.setS21login("testuser");
        existing.setSecretCode("7777");
        existing.setExpiresAt(OffsetDateTime.now());

        ProfileCodeResponse expected = new ProfileCodeResponse("testuser", "7777", existing.getExpiresAt());

        when(profileValidationRepository.findByS21login("testuser"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(profileValidationRepository.save(any(ProfileValidation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(profileVerificationMapper.toProfileCodeResponse(existing))
                .thenReturn(expected);

        ProfileCodeResponse result = profileService.getVerificationCode("testuser");

        assertNotNull(result);
        assertEquals("7777", result.getSecretCode());
        verify(profileValidationRepository).save(any(ProfileValidation.class));
        verify(profileValidationRepository, times(2)).findByS21login("testuser");
    }

    @Test
    void updateLastCommand_shouldUpdateCommand() {
        // Given
        LastCommandRequest request = new LastCommandRequest("123456", new LastCommandState(LastCommandType.SEARCH, null));
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.of(testProfile));
        when(profileRepository.save(testProfile)).thenReturn(testProfile);
        when(profileMapper.toDto(testProfile)).thenReturn(testProfileDto);

        // When
        ProfileDto result = profileService.updateLastCommand(request);

        // Then
        assertNotNull(result);
        verify(profileRepository).findByTelegramId("123456");
        verify(profileRepository).save(testProfile);
        assertEquals(new LastCommandState(LastCommandType.SEARCH, null), testProfile.getLastCommand());
    }

    @Test
    void updateLastCommand_shouldThrowException_whenProfileNotFound() {
        // Given
        LastCommandRequest request = new LastCommandRequest("123456", new LastCommandState(LastCommandType.SEARCH, null));
        when(profileRepository.findByTelegramId("123456"))
                .thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> profileService.updateLastCommand(request)
        );
        assertTrue(exception.getMessage().contains("Профиль не найден для telegramId = 123456"));
        verify(profileRepository).findByTelegramId("123456");
        verify(profileRepository, never()).save(any());
    }

    @Test
    void getCampus_shouldReturnParticipantCampus_whenApiReturnsCampus() throws ApiException {
        UUID campusId = UUID.fromString("7c293c9c-f28c-4b10-be29-560e4b000a34");
        ParticipantCampusV1DTO campusDto = mock(ParticipantCampusV1DTO.class);
        ParticipantV1DTO participant = mock(ParticipantV1DTO.class);
        ParticipantCampus campusEntity = new ParticipantCampus();
        campusEntity.setId(campusId.toString());
        campusEntity.setCampusName("Kazan");
        Participant participantEntity = new Participant();
        participantEntity.setLogin("testuser");

        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.empty());
        when(participantApi.getParticipantByLogin("testuser")).thenReturn(participant);
        when(participant.getCampus()).thenReturn(campusDto);
        when(campusDto.getId()).thenReturn(campusId);
        when(campusDto.getShortName()).thenReturn("Kazan");
        when(profileMapper.toEntity(campusDto)).thenReturn(campusEntity);
        when(profileMapper.toEntity(participant)).thenReturn(participantEntity);

        CampusDto result = profileService.getCampus("123456");

        assertNotNull(result);
        assertEquals("Kazan", result.getName());
        assertEquals(campusId.toString(), result.getUuid());
        verify(participantCampusRepository).save(campusEntity);
        verify(participantRepository).save(participantEntity);
    }

    @Test
    void getCampus_shouldFallbackToDefaultMoscow_whenApiThrows() throws ApiException {
        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.empty());
        when(participantApi.getParticipantByLogin("testuser")).thenThrow(mock(ApiException.class));

        CampusDto result = profileService.getCampus("123456");

        assertNotNull(result);
        assertEquals("Moscow", result.getName());
        assertEquals("6bfe3c56-0211-4fe1-9e59-51616caac4dd", result.getUuid());
    }

    @Test
    void getCampus_shouldThrowEntityNotFound_whenProfileMissing() {
        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> profileService.getCampus("123456"));

        assertTrue(exception.getMessage().contains("Не найден логин"));
    }

    @Test
    void getCampus_shouldFallbackToDefault_whenS21LoginBlank() {
        testProfile.setS21login("   ");
        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));

        CampusDto result = profileService.getCampus("123456");

        assertNotNull(result);
        assertEquals("Moscow", result.getName());
        assertEquals("6bfe3c56-0211-4fe1-9e59-51616caac4dd", result.getUuid());
    }

    @Test
    void getCampus_shouldFallbackToDefault_whenS21LoginNull() {
        testProfile.setS21login(null);
        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));

        CampusDto result = profileService.getCampus("123456");

        assertNotNull(result);
        assertEquals("Moscow", result.getName());
        assertEquals("6bfe3c56-0211-4fe1-9e59-51616caac4dd", result.getUuid());
    }

    @Test
    void getCampus_shouldFallbackToDefault_whenApiReturnsNullParticipant() throws ApiException {
        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.empty());
        when(participantApi.getParticipantByLogin("testuser")).thenReturn(null);

        CampusDto result = profileService.getCampus("123456");

        assertNotNull(result);
        assertEquals("Moscow", result.getName());
        assertEquals("6bfe3c56-0211-4fe1-9e59-51616caac4dd", result.getUuid());
    }

    @Test
    void getCampus_shouldUseDatabaseFirst_whenParticipantExists() {
        ParticipantCampus campus = new ParticipantCampus();
        campus.setId("7c293c9c-f28c-4b10-be29-560e4b000a34");
        campus.setCampusName("Kazan");

        Participant participant = new Participant();
        participant.setLogin("testuser");
        participant.setCampus(campus);

        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.of(participant));

        CampusDto result = profileService.getCampus("123456");

        assertNotNull(result);
        assertEquals("Kazan", result.getName());
        assertEquals("7c293c9c-f28c-4b10-be29-560e4b000a34", result.getUuid());
        verifyNoInteractions(participantApi);
    }

    @Test
    void getCampus_shouldFallbackToApi_whenStoredParticipantHasNoCampus() throws ApiException {
        Participant participant = new Participant();
        participant.setLogin("testuser");
        participant.setCampus(null);

        UUID campusId = UUID.fromString("7c293c9c-f28c-4b10-be29-560e4b000a34");
        ParticipantCampusV1DTO campusDto = mock(ParticipantCampusV1DTO.class);
        ParticipantV1DTO participantDto = mock(ParticipantV1DTO.class);
        ParticipantCampus campusEntity = new ParticipantCampus();
        campusEntity.setId(campusId.toString());
        campusEntity.setCampusName("Kazan");
        Participant participantEntity = new Participant();
        participantEntity.setLogin("testuser");

        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.of(participant));
        when(participantApi.getParticipantByLogin("testuser")).thenReturn(participantDto);
        when(participantDto.getCampus()).thenReturn(campusDto);
        when(campusDto.getShortName()).thenReturn("Kazan");
        when(campusDto.getId()).thenReturn(campusId);
        when(profileMapper.toEntity(campusDto)).thenReturn(campusEntity);
        when(profileMapper.toEntity(participantDto)).thenReturn(participantEntity);

        CampusDto result = profileService.getCampus("123456");

        assertEquals("Kazan", result.getName());
        assertEquals(campusId.toString(), result.getUuid());
        verify(participantApi).getParticipantByLogin("testuser");
    }

    @Test
    void getParticipant_shouldMapAndPersistParticipantAndCampus() throws ApiException {
        ParticipantV1DTO participantDto = mock(ParticipantV1DTO.class);
        ParticipantCampusV1DTO campusDto = mock(ParticipantCampusV1DTO.class);
        ParticipantCampus campusEntity = new ParticipantCampus();
        campusEntity.setId("campus-id");
        campusEntity.setCampusName("Moscow");
        Participant participantEntity = new Participant();
        participantEntity.setLogin("testuser");
        ParticipantDto mappedDto = ParticipantDto.builder().login("testuser").build();

        when(participantApi.getParticipantByLogin("testuser")).thenReturn(participantDto);
        when(participantDto.getCampus()).thenReturn(campusDto);
        when(profileMapper.toEntity(campusDto)).thenReturn(campusEntity);
        when(profileMapper.toEntity(participantDto)).thenReturn(participantEntity);
        when(profileMapper.toDto(participantEntity)).thenReturn(mappedDto);
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.empty());

        ParticipantDto result = profileService.getParticipant("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getLogin());
        verify(graphQLService).refreshStudentCoalitionByLogin("testuser");
        verify(participantCampusRepository).save(campusEntity);
        verify(participantRepository).save(participantEntity);
        assertSame(campusEntity, participantEntity.getCampus());
    }

    @Test
    void getParticipant_shouldIncludeCoalition_whenCoalitionExists() throws ApiException {
        ParticipantV1DTO participantDto = mock(ParticipantV1DTO.class);
        ParticipantCampusV1DTO campusDto = mock(ParticipantCampusV1DTO.class);
        ParticipantCampus campusEntity = new ParticipantCampus();
        Participant participantEntity = new Participant();
        participantEntity.setLogin("testuser");
        ParticipantDto mappedDto = ParticipantDto.builder().login("testuser").build();
        StudentCoalition coalition = new StudentCoalition();
        coalition.setLogin("testuser");
        coalition.setCoalitionName("Capybaras");
        coalition.setMemberCount(1085);
        coalition.setRank(271);

        when(participantApi.getParticipantByLogin("testuser")).thenReturn(participantDto);
        when(participantDto.getCampus()).thenReturn(campusDto);
        when(profileMapper.toEntity(campusDto)).thenReturn(campusEntity);
        when(profileMapper.toEntity(participantDto)).thenReturn(participantEntity);
        when(profileMapper.toDto(participantEntity)).thenReturn(mappedDto);
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.of(coalition));

        ParticipantDto result = profileService.getParticipant("testuser");

        assertNotNull(result);
        verify(graphQLService).refreshStudentCoalitionByLogin("testuser");
        assertNotNull(result.getCoalition());
        assertEquals("Capybaras", result.getCoalition().getName());
        assertEquals(Integer.valueOf(1085), result.getCoalition().getMemberCount());
        assertEquals(Integer.valueOf(271), result.getCoalition().getRank());
    }

    @Test
    void getCampus_shouldUseDefaultName_whenStoredCampusNameBlank() {
        ParticipantCampus campus = new ParticipantCampus();
        campus.setId("7c293c9c-f28c-4b10-be29-560e4b000a34");
        campus.setCampusName("  ");
        Participant participant = new Participant();
        participant.setLogin("testuser");
        participant.setCampus(campus);

        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.of(participant));

        CampusDto result = profileService.getCampus("123456");

        assertNotNull(result);
        assertEquals("Moscow", result.getName());
        assertEquals("7c293c9c-f28c-4b10-be29-560e4b000a34", result.getUuid());
    }

    @Test
    void getCampus_shouldUseDefaultName_whenStoredCampusNameNull() {
        ParticipantCampus campus = new ParticipantCampus();
        campus.setId("7c293c9c-f28c-4b10-be29-560e4b000a34");
        campus.setCampusName(null);
        Participant participant = new Participant();
        participant.setLogin("testuser");
        participant.setCampus(campus);

        when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.of(participant));

        CampusDto result = profileService.getCampus("123456");

        assertNotNull(result);
        assertEquals("Moscow", result.getName());
        assertEquals("7c293c9c-f28c-4b10-be29-560e4b000a34", result.getUuid());
    }
}
