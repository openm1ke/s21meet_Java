package ru.izpz.edu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.izpz.dto.CampusDto;
import ru.izpz.dto.LastCommandRequest;
import ru.izpz.dto.LastCommandState;
import ru.izpz.dto.LastCommandType;
import ru.izpz.dto.ParticipantCoalitionDto;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.dto.ProfileCodeResponse;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.ProjectsDto;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.exception.EntityNotFoundException;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.mapper.ProfileMapper;
import ru.izpz.edu.mapper.ProfileVerificationMapper;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Online;
import ru.izpz.edu.model.Participant;
import ru.izpz.edu.model.ParticipantCampus;
import ru.izpz.edu.model.Profile;
import ru.izpz.edu.model.ProfileValidation;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.repository.OnlineRepository;
import ru.izpz.edu.repository.ParticipantRepository;
import ru.izpz.edu.repository.ProfileRepository;
import ru.izpz.edu.repository.ProfileValidationRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

  @Mock private ProfileMapper profileMapper;

  @Mock private ProfileVerificationMapper profileVerificationMapper;

  @Mock private ProfileRepository profileRepository;

  @Mock private ProfileValidationRepository profileValidationRepository;

  @Mock private ParticipantSyncService participantSyncService;
  @Mock private ParticipantCoalitionService participantCoalitionService;

  @Mock private ParticipantRepository participantRepository;

  @Mock private CampusService campusService;

  @Mock private CampusCatalog campusCatalog;
  @Mock private WorkplaceRepository workplaceRepository;
  @Mock private OnlineRepository onlineRepository;
  @Mock private ClusterRepository clusterRepository;
  @InjectMocks private ProfileService profileService;

  private Profile testProfile;
  private ProfileDto testProfileDto;

  @BeforeEach
  void setUp() {
    testProfile = new Profile();
    testProfile.setId(java.util.UUID.randomUUID());
    testProfile.setTelegramId("123456");
    testProfile.setS21login("testuser");
    testProfile.setStatus(ProfileStatus.CREATED);

    testProfileDto =
        new ProfileDto(
            testProfile.getTelegramId(), testProfile.getS21login(), testProfile.getStatus(), null);

    lenient()
        .when(profileMapper.toDto(any(Profile.class)))
        .thenAnswer(
            inv -> {
              Profile p = inv.getArgument(0);
              return new ProfileDto(
                  p.getTelegramId(), p.getS21login(), p.getStatus(), p.getLastCommand());
            });
    lenient()
        .when(campusCatalog.targetCampusIds())
        .thenReturn(
            java.util.List.of(
                "6bfe3c56-0211-4fe1-9e59-51616caac4dd", "7c293c9c-f28c-4b10-be29-560e4b000a34"));
    lenient()
        .when(campusCatalog.campusName("6bfe3c56-0211-4fe1-9e59-51616caac4dd"))
        .thenReturn("MSK");
    lenient()
        .when(campusCatalog.campusName("7c293c9c-f28c-4b10-be29-560e4b000a34"))
        .thenReturn("KZN");
    lenient().when(participantRepository.findByLogin(anyString())).thenReturn(Optional.empty());
    lenient().when(workplaceRepository.findByLogin(anyString())).thenReturn(Optional.empty());
    lenient().when(onlineRepository.findByLogin(anyString())).thenReturn(Optional.empty());
  }

  @Test
  void getOrCreateProfile_shouldReturnExistingProfile_whenFound() {
    // Given
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(profileMapper.toDto(testProfile)).thenReturn(testProfileDto);

    // When
    ProfileDto result = profileService.getOrCreateProfile("123456");

    // Then
    assertNotNull(result);
    assertEquals(testProfileDto, result);
    verify(profileRepository).findByTelegramId("123456");
    verifyNoInteractions(participantCoalitionService);
    verify(profileRepository, never()).save(any());
  }

  @Test
  void getOrCreateProfile_shouldCreateNewProfile_whenNotFound() {
    // Given
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.empty());
    when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
    when(profileMapper.toDto(testProfile)).thenReturn(testProfileDto);

    // When
    ProfileDto result = profileService.getOrCreateProfile("123456");

    // Then
    assertNotNull(result);
    assertEquals(testProfileDto, result);
    verify(profileRepository).findByTelegramId("123456");
    verifyNoInteractions(participantCoalitionService);
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
    verifyNoInteractions(participantCoalitionService);
  }

  @Test
  void getOrCreateProfile_shouldFallbackToRead_whenSaveRaceConditionHappens() {
    when(profileRepository.findByTelegramId("123456"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(testProfile));
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
        .thenReturn(Optional.empty())
        .thenReturn(Optional.empty());
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
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
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
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(EntityNotFoundException.class, () -> profileService.getProfile("123456"));
    assertTrue(exception.getMessage().contains("Профиль не найден для telegramId = 123456"));
    verify(profileRepository).findByTelegramId("123456");
  }

  @Test
  void updateProfileStatus_shouldUpdateStatus_whenProfileExists() {
    // Given
    ProfileRequest request =
        ProfileRequest.builder().telegramId("123456").status(ProfileStatus.CONFIRMED).build();
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
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
    ProfileRequest request =
        ProfileRequest.builder().telegramId("123456").status(ProfileStatus.CONFIRMED).build();
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class, () -> profileService.updateProfileStatus(request));
    assertTrue(exception.getMessage().contains("Профиль не найден для telegramId = 123456"));
    verify(profileRepository).findByTelegramId("123456");
    verify(profileRepository, never()).save(any());
  }

  @Test
  void checkAndSetLogin_shouldSetLogin_whenValid() {
    // Given
    testProfile.setS21login(null);
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(profileRepository.existsByS21login("newuser")).thenReturn(false);
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

    when(profileRepository.findByTelegramId("123456"))
        .thenReturn(Optional.of(testProfile))
        .thenReturn(Optional.of(savedByAnotherTx));
    when(profileRepository.existsByS21login("newuser")).thenReturn(false);
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

    when(profileRepository.findByTelegramId("123456"))
        .thenReturn(Optional.of(testProfile))
        .thenReturn(Optional.empty());
    when(profileRepository.existsByS21login("newuser")).thenReturn(false);
    when(profileRepository.save(testProfile))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    ProfileDto result = profileService.checkAndSetLogin("123456", "newuser");

    assertNotNull(result);
    assertEquals("newuser", result.s21login());
    verify(profileRepository).save(testProfile);
    verify(profileRepository, times(2)).findByTelegramId("123456");
  }

  @Test
  void checkAndSetLogin_shouldNotWarmUp_whenFallbackProfileHasDifferentLogin() {
    testProfile.setS21login(null);
    Profile savedByAnotherTx = new Profile();
    savedByAnotherTx.setTelegramId("123456");
    savedByAnotherTx.setS21login("another-login");
    savedByAnotherTx.setStatus(ProfileStatus.CREATED);
    ProfileDto differentLoginDto =
        new ProfileDto("123456", "another-login", ProfileStatus.CREATED, null);

    when(profileRepository.findByTelegramId("123456"))
        .thenReturn(Optional.of(testProfile))
        .thenReturn(Optional.of(savedByAnotherTx));
    when(profileRepository.existsByS21login("newuser")).thenReturn(false);
    when(profileRepository.save(testProfile))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));
    when(profileMapper.toDto(savedByAnotherTx)).thenReturn(differentLoginDto);

    ProfileDto result = profileService.checkAndSetLogin("123456", "newuser");

    assertNotNull(result);
    assertEquals("another-login", result.s21login());
    verify(participantSyncService, never()).syncByEduLogin("newuser");
    verify(participantCoalitionService, never()).refreshByLogin("newuser");
  }

  @Test
  void checkAndSetLogin_shouldThrowException_whenLoginAlreadyExists() {
    // Given
    testProfile.setS21login(null);
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(profileRepository.existsByS21login("existinguser")).thenReturn(true);

    // When & Then
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> profileService.checkAndSetLogin("123456", "existinguser"));
    assertTrue(
        exception.getMessage().contains("Логин existinguser уже привязан к другому профилю"));
    verify(profileRepository).findByTelegramId("123456");
    verify(profileRepository).existsByS21login("existinguser");
    verify(profileRepository, never()).save(any());
  }

  @Test
  void checkAndSetLogin_shouldThrowException_whenProfileAlreadyHasLogin() {
    // Given
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));

    // When & Then
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> profileService.checkAndSetLogin("123456", "anotheruser"));
    assertTrue(exception.getMessage().contains("Профиль уже привязан к логину testuser"));
    verify(profileRepository).findByTelegramId("123456");
    verify(profileRepository, never()).existsByS21login(anyString());
    verify(profileRepository, never()).save(any());
  }

  @Test
  void checkAndSetLogin_shouldThrowException_whenProfileNotFound() {
    // Given
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> profileService.checkAndSetLogin("123456", "newuser"));
    assertTrue(exception.getMessage().contains("Профиль не найден для telegramId = 123456"));
    verify(profileRepository).findByTelegramId("123456");
    verify(profileRepository, never()).existsByS21login(anyString());
    verify(profileRepository, never()).save(any());
  }

  @Test
  void checkAndSetLogin_shouldReturnProfile_whenSameLoginAlreadyBoundToSameTelegram() {
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(profileMapper.toDto(testProfile)).thenReturn(testProfileDto);

    ProfileDto result = profileService.checkAndSetLogin("123456", "testuser");

    assertNotNull(result);
    assertEquals("testuser", result.s21login());
    verify(profileRepository).findByTelegramId("123456");
    verify(profileRepository, never()).existsByS21login(anyString());
    verify(profileRepository, never()).save(any());
  }

  @Test
  void checkAndSetLogin_shouldReturnSavedProfile_whenWarmUpSyncThrowsRuntime() {
    testProfile.setS21login(null);
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(profileRepository.existsByS21login("newuser")).thenReturn(false);
    when(profileRepository.save(testProfile)).thenReturn(testProfile);
    when(participantSyncService.syncByEduLogin("newuser"))
        .thenThrow(new RuntimeException("sync failed"));

    ProfileDto result = profileService.checkAndSetLogin("123456", "newuser");

    assertNotNull(result);
    assertEquals("newuser", result.s21login());
    verify(participantSyncService).syncByEduLogin("newuser");
    verify(participantCoalitionService).refreshByLogin("newuser");
  }

  @Test
  void getVerificationCode_shouldReturnExistingCode_whenExists() {
    // Given
    ProfileValidation validation = new ProfileValidation();
    validation.setS21login("testuser");
    validation.setSecretCode("1234");
    validation.setExpiresAt(OffsetDateTime.now());

    ProfileCodeResponse expectedResponse =
        new ProfileCodeResponse("testuser", "1234", validation.getExpiresAt());

    when(profileValidationRepository.findByS21login("testuser"))
        .thenReturn(Optional.of(validation));
    when(profileVerificationMapper.toProfileCodeResponse(validation)).thenReturn(expectedResponse);

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

    ProfileCodeResponse expectedResponse =
        new ProfileCodeResponse("testuser", "5678", validation.getExpiresAt());

    when(profileValidationRepository.findByS21login("testuser")).thenReturn(Optional.empty());
    when(profileValidationRepository.save(any(ProfileValidation.class))).thenReturn(validation);
    when(profileVerificationMapper.toProfileCodeResponse(validation)).thenReturn(expectedResponse);

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

    ProfileCodeResponse expected =
        new ProfileCodeResponse("testuser", "7777", existing.getExpiresAt());

    when(profileValidationRepository.findByS21login("testuser"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existing));
    when(profileValidationRepository.save(any(ProfileValidation.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));
    when(profileVerificationMapper.toProfileCodeResponse(existing)).thenReturn(expected);

    ProfileCodeResponse result = profileService.getVerificationCode("testuser");

    assertNotNull(result);
    assertEquals("7777", result.getSecretCode());
    verify(profileValidationRepository).save(any(ProfileValidation.class));
    verify(profileValidationRepository, times(2)).findByS21login("testuser");
  }

  @Test
  void updateLastCommand_shouldUpdateCommand() {
    // Given
    LastCommandRequest request =
        new LastCommandRequest("123456", new LastCommandState(LastCommandType.SEARCH, null));
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
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
    LastCommandRequest request =
        new LastCommandRequest("123456", new LastCommandState(LastCommandType.SEARCH, null));
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class, () -> profileService.updateLastCommand(request));
    assertTrue(exception.getMessage().contains("Профиль не найден для telegramId = 123456"));
    verify(profileRepository).findByTelegramId("123456");
    verify(profileRepository, never()).save(any());
  }

  @Test
  void checkEduLogin_shouldMapFetchedParticipantFromApi() {
    ParticipantV1DTO fetched = new ParticipantV1DTO();
    fetched.setLogin("testuser");
    ParticipantDto mapped = ParticipantDto.builder().login("testuser").build();

    when(participantSyncService.fetchByEduLogin("testuser")).thenReturn(fetched);
    when(profileMapper.toDto(fetched)).thenReturn(mapped);

    ParticipantDto result = profileService.checkEduLogin("testuser");

    assertNotNull(result);
    assertEquals("testuser", result.getLogin());
    verify(participantSyncService).fetchByEduLogin("testuser");
    verify(profileMapper).toDto(fetched);
  }

  @Test
  void getCampus_shouldReturnParticipantCampus_whenApiReturnsCampus() {
    ParticipantCampus campusEntity = new ParticipantCampus();
    campusEntity.setId("7c293c9c-f28c-4b10-be29-560e4b000a34");
    campusEntity.setCampusName("Kazan");
    Participant participantEntity = new Participant();
    participantEntity.setLogin("testuser");
    participantEntity.setCampus(campusEntity);

    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participantEntity);

    CampusDto result = profileService.getCampus("123456");

    assertNotNull(result);
    assertEquals("Kazan", result.getName());
    assertEquals("7c293c9c-f28c-4b10-be29-560e4b000a34", result.getUuid());
    verify(participantSyncService).getOrSyncByEduLogin("testuser");
  }

  @Test
  void getCampus_shouldFallbackToDefaultMoscow_whenApiThrows() {
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(participantSyncService.getOrSyncByEduLogin("testuser"))
        .thenThrow(new PlatformClientException("boom", null));

    CampusDto result = profileService.getCampus("123456");

    assertNotNull(result);
    assertEquals("Moscow", result.getName());
    assertEquals("6bfe3c56-0211-4fe1-9e59-51616caac4dd", result.getUuid());
  }

  @Test
  void getCampus_shouldThrowEntityNotFound_whenProfileMissing() {
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.empty());

    EntityNotFoundException exception =
        assertThrows(EntityNotFoundException.class, () -> profileService.getCampus("123456"));

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
  void getCampus_shouldFallbackToDefault_whenApiReturnsNullParticipant() {
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(participantSyncService.getOrSyncByEduLogin("testuser"))
        .thenThrow(new IllegalStateException("Участник не найден"));

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
    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participant);

    CampusDto result = profileService.getCampus("123456");

    assertNotNull(result);
    assertEquals("Kazan", result.getName());
    assertEquals("7c293c9c-f28c-4b10-be29-560e4b000a34", result.getUuid());
    verify(participantSyncService).getOrSyncByEduLogin("testuser");
  }

  @Test
  void getCampus_shouldFallbackToApi_whenStoredParticipantHasNoCampus() {
    ParticipantCampus campusEntity = new ParticipantCampus();
    campusEntity.setId("7c293c9c-f28c-4b10-be29-560e4b000a34");
    campusEntity.setCampusName("Kazan");
    Participant participantEntity = new Participant();
    participantEntity.setLogin("testuser");
    participantEntity.setCampus(campusEntity);

    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participantEntity);

    CampusDto result = profileService.getCampus("123456");

    assertEquals("Kazan", result.getName());
    assertEquals("7c293c9c-f28c-4b10-be29-560e4b000a34", result.getUuid());
    verify(participantSyncService).getOrSyncByEduLogin("testuser");
  }

  @Test
  void getCampus_shouldFallbackToDefault_whenParticipantCampusIsNull() {
    Participant participant = new Participant();
    participant.setLogin("testuser");
    participant.setCampus(null);

    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participant);

    CampusDto result = profileService.getCampus("123456");

    assertNotNull(result);
    assertEquals("Moscow", result.getName());
    assertEquals("6bfe3c56-0211-4fe1-9e59-51616caac4dd", result.getUuid());
  }

  @Test
  void getParticipant_shouldMapAndPersistParticipantAndCampus() {
    Participant participantEntity = new Participant();
    participantEntity.setLogin("testuser");
    final ParticipantDto mappedDto = ParticipantDto.builder().login("testuser").build();

    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participantEntity);
    when(profileMapper.toDto(participantEntity)).thenReturn(mappedDto);

    ParticipantDto result = profileService.getParticipant("testuser");

    assertNotNull(result);
    assertEquals("testuser", result.getLogin());
    assertEquals(Boolean.FALSE, result.getIsOnline());
    InOrder inOrder = inOrder(participantSyncService, participantCoalitionService);
    inOrder.verify(participantSyncService).getOrSyncByEduLogin("testuser");
    inOrder.verify(participantCoalitionService).enrichParticipant(mappedDto, "testuser");
  }

  @Test
  void getParticipant_shouldIncludeCoalition_whenCoalitionExists() {
    Participant participantEntity = new Participant();
    participantEntity.setLogin("testuser");
    final ParticipantDto mappedDto = ParticipantDto.builder().login("testuser").build();
    doAnswer(
            invocation -> {
              ParticipantDto dto = invocation.getArgument(0);
              dto.setCoalition(new ParticipantCoalitionDto("Capybaras", 1085, 271));
              return null;
            })
        .when(participantCoalitionService)
        .enrichParticipant(mappedDto, "testuser");

    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participantEntity);
    when(profileMapper.toDto(participantEntity)).thenReturn(mappedDto);

    ParticipantDto result = profileService.getParticipant("testuser");

    assertNotNull(result);
    verify(participantCoalitionService).enrichParticipant(mappedDto, "testuser");
    assertNotNull(result.getCoalition());
    assertEquals("Capybaras", result.getCoalition().getName());
    assertEquals(Integer.valueOf(1085), result.getCoalition().getMemberCount());
    assertEquals(Integer.valueOf(271), result.getCoalition().getRank());
  }

  @Test
  void getParticipant_shouldIncludeSeatData_whenOnlineInCampus() {
    Participant participantEntity = new Participant();
    participantEntity.setLogin("testuser");
    final ParticipantDto mappedDto = ParticipantDto.builder().login("testuser").build();
    Workplace workplace = new Workplace();
    workplace.setId(new WorkplaceId(42L, "A", 7));
    workplace.setStageGroupName("Core");
    workplace.setStageName("C3");
    Cluster cluster = new Cluster();
    cluster.setClusterId(42L);
    cluster.setName("Main Cluster");

    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participantEntity);
    when(profileMapper.toDto(participantEntity)).thenReturn(mappedDto);
    when(workplaceRepository.findByLogin("testuser")).thenReturn(Optional.of(workplace));
    when(clusterRepository.findById(42L)).thenReturn(Optional.of(cluster));

    ParticipantDto result = profileService.getParticipant("testuser");

    assertNotNull(result);
    assertEquals(Boolean.TRUE, result.getIsOnline());
    assertNotNull(result.getSeat());
    assertEquals("Main Cluster", result.getSeat().getClusterName());
    assertEquals("A", result.getSeat().getRow());
    assertEquals(Integer.valueOf(7), result.getSeat().getNumber());
    assertEquals("Core", result.getSeat().getStageGroupName());
    assertEquals("C3", result.getSeat().getStageName());
    assertNull(result.getLastSeenAt());
  }

  @Test
  void getParticipant_shouldIncludeLastSeen_whenOffline() {
    Participant participantEntity = new Participant();
    participantEntity.setLogin("testuser");
    final ParticipantDto mappedDto = ParticipantDto.builder().login("testuser").build();
    Online online = new Online();
    online.setLogin("testuser");
    online.setIsOnline(false);
    OffsetDateTime seenAt = OffsetDateTime.now().minusMinutes(3);
    online.setLastSeenAt(seenAt);

    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participantEntity);
    when(profileMapper.toDto(participantEntity)).thenReturn(mappedDto);
    when(workplaceRepository.findByLogin("testuser")).thenReturn(Optional.empty());
    when(onlineRepository.findByLogin("testuser")).thenReturn(Optional.of(online));

    ParticipantDto result = profileService.getParticipant("testuser");

    assertNotNull(result);
    assertEquals(Boolean.FALSE, result.getIsOnline());
    assertEquals(seenAt, result.getLastSeenAt());
  }

  @Test
  void getParticipant_shouldIncludeSeatWithoutId_whenOnlineSeatHasNoId() {
    Participant participantEntity = new Participant();
    participantEntity.setLogin("testuser");
    final ParticipantDto mappedDto = ParticipantDto.builder().login("testuser").build();
    Workplace workplace = new Workplace();
    workplace.setId(null);
    workplace.setStageGroupName("Core");
    workplace.setStageName("C3");

    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participantEntity);
    when(profileMapper.toDto(participantEntity)).thenReturn(mappedDto);
    when(workplaceRepository.findByLogin("testuser")).thenReturn(Optional.of(workplace));

    ParticipantDto result = profileService.getParticipant("testuser");

    assertNotNull(result);
    assertEquals(Boolean.TRUE, result.getIsOnline());
    assertNotNull(result.getSeat());
    assertNull(result.getSeat().getRow());
    assertNull(result.getSeat().getNumber());
    assertNull(result.getSeat().getClusterName());
    verify(clusterRepository, never()).findById(anyLong());
  }

  @Test
  void getParticipant_shouldNotResolveCluster_whenSeatClusterIdIsNull() {
    Participant participantEntity = new Participant();
    participantEntity.setLogin("testuser");
    final ParticipantDto mappedDto = ParticipantDto.builder().login("testuser").build();
    Workplace workplace = new Workplace();
    workplace.setId(new WorkplaceId(null, "B", 2));
    workplace.setStageGroupName("Core");
    workplace.setStageName("C3");

    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participantEntity);
    when(profileMapper.toDto(participantEntity)).thenReturn(mappedDto);
    when(workplaceRepository.findByLogin("testuser")).thenReturn(Optional.of(workplace));

    ParticipantDto result = profileService.getParticipant("testuser");

    assertNotNull(result);
    assertEquals(Boolean.TRUE, result.getIsOnline());
    assertEquals("B", result.getSeat().getRow());
    assertEquals(Integer.valueOf(2), result.getSeat().getNumber());
    assertNull(result.getSeat().getClusterName());
    verify(clusterRepository, never()).findById(anyLong());
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
    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participant);

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
    when(participantSyncService.getOrSyncByEduLogin("testuser")).thenReturn(participant);

    CampusDto result = profileService.getCampus("123456");

    assertNotNull(result);
    assertEquals("Moscow", result.getName());
    assertEquals("7c293c9c-f28c-4b10-be29-560e4b000a34", result.getUuid());
  }

  @Test
  void getProjectNamesByTelegramId_shouldReturnDistinctProjectNamesForBoundLogin() {
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(testProfile));
    when(campusService.getStudentProjectsByLogin("testuser"))
        .thenReturn(
            List.of(
                new ProjectsDto(
                    "1", "A1_Maze", null, null, null, null, null, null, null, null, null),
                new ProjectsDto(
                    "2", "A1_Maze", null, null, null, null, null, null, null, null, null),
                new ProjectsDto(
                    "3",
                    "  C2_SimpleBashUtils  ",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null),
                new ProjectsDto("4", " ", null, null, null, null, null, null, null, null, null),
                new ProjectsDto("5", null, null, null, null, null, null, null, null, null, null)));

    List<String> result = profileService.getProjectNamesByTelegramId("123456");

    assertEquals(List.of("A1_Maze", "C2_SimpleBashUtils"), result);
    verify(campusService).getStudentProjectsByLogin("testuser");
  }

  @Test
  void getProjectNamesByTelegramId_shouldReturnEmptyWhenProfileMissingOrLoginBlank() {
    when(profileRepository.findByTelegramId("missing")).thenReturn(Optional.empty());

    List<String> missing = profileService.getProjectNamesByTelegramId("missing");
    assertTrue(missing.isEmpty());

    Profile blankLoginProfile = new Profile();
    blankLoginProfile.setTelegramId("123456");
    blankLoginProfile.setS21login("  ");
    when(profileRepository.findByTelegramId("123456")).thenReturn(Optional.of(blankLoginProfile));

    List<String> blank = profileService.getProjectNamesByTelegramId("123456");
    assertTrue(blank.isEmpty());
    verify(campusService, never()).getStudentProjectsByLogin(anyString());
  }
}
