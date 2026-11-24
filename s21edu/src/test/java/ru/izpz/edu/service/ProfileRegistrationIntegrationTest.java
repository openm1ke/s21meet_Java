package ru.izpz.edu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.izpz.dto.ProfileCodeResponse;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.RegistrationErrorDto;
import ru.izpz.edu.BaseTestH2;
import ru.izpz.edu.exception.RegistrationValidationException;
import ru.izpz.edu.mapper.ProfileMapperImpl;
import ru.izpz.edu.mapper.ProfileVerificationMapperImpl;
import ru.izpz.edu.model.Profile;
import ru.izpz.edu.model.ProfileValidation;
import ru.izpz.edu.repository.ProfileRepository;
import ru.izpz.edu.repository.ProfileValidationRepository;

@Import({ProfileService.class, ProfileMapperImpl.class, ProfileVerificationMapperImpl.class})
@TestPropertySource(properties = "profile.service.enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProfileRegistrationIntegrationTest extends BaseTestH2 {

  private static final String TELEGRAM_ID = "123456";
  private static final String LOGIN = "legacyuser";

  @Autowired private ProfileService profileService;
  @Autowired private ProfileRepository profileRepository;
  @Autowired private ProfileValidationRepository profileValidationRepository;

  @MockitoBean private ParticipantSyncService participantSyncService;
  @MockitoBean private ParticipantCoalitionService participantCoalitionService;
  @MockitoBean private CampusService campusService;

  @BeforeEach
  void clearProfiles() {
    profileValidationRepository.deleteAll();
    profileRepository.deleteAll();
  }

  @ParameterizedTest
  @EnumSource(
      value = ProfileStatus.class,
      names = {"CREATED", "REGISTRATION", "VALIDATION"})
  void legacyBoundProfile_canRestartAndConfirmOnlyWithCorrectCode(ProfileStatus initialStatus) {
    saveProfile(initialStatus, LOGIN);
    saveCode(OffsetDateTime.now().plusDays(3));
    updateStatus(ProfileStatus.REGISTRATION);

    profileService.checkAndSetLogin(TELEGRAM_ID, LOGIN);
    Profile pending = loadProfile();
    assertNull(pending.getS21login());
    assertEquals(LOGIN, pending.getPendingS21login());
    assertNotNull(pending.getPendingRegistrationExpiresAt());

    Profile anotherProfile = new Profile();
    anotherProfile.setTelegramId("654321");
    anotherProfile.setStatus(ProfileStatus.CREATED);
    profileRepository.saveAndFlush(anotherProfile);
    assertThrows(
        IllegalStateException.class, () -> profileService.checkAndSetLogin("654321", LOGIN));

    final ProfileCodeResponse code = profileService.getVerificationCode(LOGIN);
    updateStatus(ProfileStatus.VALIDATION);

    assertThrows(
        IllegalStateException.class,
        () -> profileService.confirmRegistration(TELEGRAM_ID, "wrong-code"));
    assertEquals(ProfileStatus.VALIDATION, loadProfile().getStatus());

    ProfileDto confirmed = profileService.confirmRegistration(TELEGRAM_ID, code.getSecretCode());
    assertEquals(ProfileStatus.CONFIRMED, confirmed.status());
    Profile saved = loadProfile();
    assertEquals(LOGIN, saved.getS21login());
    assertNull(saved.getPendingS21login());
    assertNull(saved.getPendingRegistrationExpiresAt());
    assertFalse(profileValidationRepository.findByS21login(LOGIN).isPresent());

    assertEquals(ProfileStatus.CONFIRMED, updateStatus(ProfileStatus.REGISTRATION).status());
    assertEquals(
        ProfileStatus.CONFIRMED, profileService.checkAndSetLogin(TELEGRAM_ID, LOGIN).status());
    assertEquals(ProfileStatus.CONFIRMED, loadProfile().getStatus());
  }

  @Test
  void missingAttempt_returnsDistinctErrorAndCommitsResetWithoutConfirmingLegacyLogin() {
    saveProfile(ProfileStatus.VALIDATION, LOGIN);

    RegistrationValidationException error =
        assertThrows(
            RegistrationValidationException.class,
            () -> profileService.confirmRegistration(TELEGRAM_ID, "1234"));

    assertEquals(RegistrationErrorDto.REGISTRATION_NOT_STARTED_CODE, error.getCode());
    assertNull(error.getExpiresAt());
    assertEquals(ProfileStatus.CREATED, loadProfile().getStatus());
    assertEquals(LOGIN, loadProfile().getS21login());
  }

  @Test
  void missingAttemptDeadline_returnsDistinctErrorAndClearsPendingCode() {
    Profile profile = saveProfile(ProfileStatus.VALIDATION, null);
    profile.setPendingS21login(LOGIN);
    profileRepository.saveAndFlush(profile);
    saveCode(OffsetDateTime.now().plusDays(1));

    RegistrationValidationException error =
        assertThrows(
            RegistrationValidationException.class,
            () -> profileService.confirmRegistration(TELEGRAM_ID, "1234"));

    assertEquals(RegistrationErrorDto.REGISTRATION_NOT_STARTED_CODE, error.getCode());
    assertEquals(ProfileStatus.CREATED, loadProfile().getStatus());
    assertNull(loadProfile().getPendingS21login());
    assertFalse(profileValidationRepository.findByS21login(LOGIN).isPresent());
  }

  @Test
  void expiredAttempt_keepsExpiryClassificationAndCommitsReset() {
    Profile profile = saveProfile(ProfileStatus.VALIDATION, null);
    profile.setPendingS21login(LOGIN);
    profile.setPendingRegistrationExpiresAt(OffsetDateTime.now().minusMinutes(1));
    profileRepository.saveAndFlush(profile);
    saveCode(OffsetDateTime.now().plusDays(1));

    RegistrationValidationException error =
        assertThrows(
            RegistrationValidationException.class,
            () -> profileService.confirmRegistration(TELEGRAM_ID, "1234"));

    assertEquals(RegistrationErrorDto.REGISTRATION_EXPIRED_CODE, error.getCode());
    assertNotNull(error.getExpiresAt());
    assertEquals(ProfileStatus.CREATED, loadProfile().getStatus());
    assertFalse(profileValidationRepository.findByS21login(LOGIN).isPresent());
  }

  @Test
  void confirmedProfile_duplicateConfirmationDoesNotResetRegistration() {
    saveProfile(ProfileStatus.CONFIRMED, LOGIN);

    ProfileDto result = profileService.confirmRegistration(TELEGRAM_ID, "old-code");

    assertEquals(ProfileStatus.CONFIRMED, result.status());
    assertEquals(ProfileStatus.CONFIRMED, loadProfile().getStatus());
    assertEquals(LOGIN, loadProfile().getS21login());
  }

  private Profile saveProfile(ProfileStatus status, String login) {
    Profile profile = new Profile();
    profile.setTelegramId(TELEGRAM_ID);
    profile.setS21login(login);
    profile.setStatus(status);
    return profileRepository.saveAndFlush(profile);
  }

  private void saveCode(OffsetDateTime expiresAt) {
    ProfileValidation code = new ProfileValidation();
    code.setS21login(LOGIN);
    code.setSecretCode("1234");
    code.setExpiresAt(expiresAt);
    profileValidationRepository.saveAndFlush(code);
  }

  private Profile loadProfile() {
    return profileRepository.findByTelegramId(TELEGRAM_ID).orElseThrow();
  }

  private ProfileDto updateStatus(ProfileStatus status) {
    return profileService.updateProfileStatus(
        ProfileRequest.builder().telegramId(TELEGRAM_ID).status(status).build());
  }
}
