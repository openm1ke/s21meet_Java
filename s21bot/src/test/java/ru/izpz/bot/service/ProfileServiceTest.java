package ru.izpz.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.bot.client.ProfileClient;
import ru.izpz.bot.client.RocketChatClient;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.RocketChatSendException;
import ru.izpz.dto.CampusRequest;
import ru.izpz.dto.CampusResponse;
import ru.izpz.dto.EventDto;
import ru.izpz.dto.EventsSliceDto;
import ru.izpz.dto.FriendDto;
import ru.izpz.dto.FriendRequest;
import ru.izpz.dto.FriendsSliceDto;
import ru.izpz.dto.LastCommandRequest;
import ru.izpz.dto.LastCommandState;
import ru.izpz.dto.LastCommandType;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.dto.ParticipantRequest;
import ru.izpz.dto.ParticipantStatusEnum;
import ru.izpz.dto.ProfileCodeRequest;
import ru.izpz.dto.ProfileCodeResponse;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.ProjectsDto;
import ru.izpz.dto.RocketChatSendRequest;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.dto.ServiceErrorDto;
import ru.izpz.utils.FeignErrorParser;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

  @Mock private ProfileClient profileClient;

  @Mock private RocketChatClient rocketChatClient;

  @InjectMocks private ProfileService profileService;

  private static final Long CHAT_ID = 12345L;
  private static final String LOGIN = "test.login";
  private static final String S21LOGIN = "s21.test.login";
  private static final String SECRET_CODE = "123456";

  @BeforeEach
  void setUp() {
    // Empty setUp method - test initialization is handled by @ExtendWith(MockitoExtension.class)
    // and field injection with @Mock and @InjectMocks annotations
  }

  @Test
  void getProfileSuccess() {
    ProfileDto expectedProfile = createTestProfile();
    when(profileClient.getOrCreateProfile(CHAT_ID.toString())).thenReturn(expectedProfile);

    ProfileDto result = profileService.getProfile(CHAT_ID);

    assertNotNull(result);
    assertEquals(expectedProfile, result);
    verify(profileClient).getOrCreateProfile(CHAT_ID.toString());
  }

  @Test
  void getProfileFeignException() {
    FeignException feignException = createFeignException(500, "Internal Server Error");
    when(profileClient.getOrCreateProfile(CHAT_ID.toString())).thenThrow(feignException);

    assertThrows(FeignException.class, () -> profileService.getProfile(CHAT_ID));
    verify(profileClient).getOrCreateProfile(CHAT_ID.toString());
  }

  @Test
  void checkEduLoginFeignExceptionThrowsEduLoginCheckException() {
    FeignException feignException = createFeignException(400, "Bad Request");
    ServiceErrorDto errorResponse =
        new ServiceErrorDto()
            .setStatus(400)
            .setExceptionUUID("test-uuid")
            .setCode("login_NOT_FOUND")
            .setMessage("Login not found");

    when(profileClient.checkEduLogin(LOGIN)).thenThrow(feignException);

    try (MockedStatic<FeignErrorParser> parserMock = mockStatic(FeignErrorParser.class)) {
      parserMock.when(() -> FeignErrorParser.parse(feignException)).thenReturn(errorResponse);

      EduLoginCheckException exception =
          assertThrows(EduLoginCheckException.class, () -> profileService.checkEduLogin(LOGIN));

      assertEquals(errorResponse, exception.getError());
      verify(profileClient).checkEduLogin(LOGIN);
    }
  }

  @Test
  void updateProfileStatusSuccess() {
    ProfileStatus status = ProfileStatus.CONFIRMED;
    ProfileDto expectedProfile = createTestProfile();

    when(profileClient.updateProfileStatus(any(ProfileRequest.class))).thenReturn(expectedProfile);

    ProfileDto result = profileService.updateProfileStatus(CHAT_ID, status);

    assertNotNull(result);
    assertEquals(expectedProfile, result);
    verify(profileClient)
        .updateProfileStatus(
            argThat(
                request ->
                    request.getTelegramId().equals(CHAT_ID.toString())
                        && request.getStatus() == status));
  }

  @Test
  void updateProfileStatusFeignException() {
    ProfileStatus status = ProfileStatus.CONFIRMED;
    FeignException feignException = createFeignException(500, "Internal Server Error");

    when(profileClient.updateProfileStatus(any(ProfileRequest.class))).thenThrow(feignException);

    assertThrows(FeignException.class, () -> profileService.updateProfileStatus(CHAT_ID, status));
    verify(profileClient).updateProfileStatus(any(ProfileRequest.class));
  }

  @Test
  void checkAndSetLoginSuccess() {
    ProfileDto expectedProfile = createTestProfile();

    when(profileClient.checkAndSetLogin(any(ProfileRequest.class))).thenReturn(expectedProfile);

    ProfileDto result = profileService.checkAndSetLogin(CHAT_ID, LOGIN);

    assertNotNull(result);
    assertEquals(expectedProfile, result);
    verify(profileClient)
        .checkAndSetLogin(
            argThat(
                request ->
                    request.getTelegramId().equals(CHAT_ID.toString())
                        && request.getS21login().equals(LOGIN)));
  }

  @Test
  void checkAndSetLoginFeignException() {
    FeignException feignException = createFeignException(500, "Internal Server Error");

    when(profileClient.checkAndSetLogin(any(ProfileRequest.class))).thenThrow(feignException);

    assertThrows(FeignException.class, () -> profileService.checkAndSetLogin(CHAT_ID, LOGIN));
    verify(profileClient).checkAndSetLogin(any(ProfileRequest.class));
  }

  @Test
  void getVerificationCodeSuccess() {
    ProfileCodeResponse response =
        new ProfileCodeResponse(S21LOGIN, SECRET_CODE, OffsetDateTime.now());
    when(profileClient.getProfileCode(any(ProfileCodeRequest.class))).thenReturn(response);

    ProfileCodeResponse result = profileService.getVerificationCode(S21LOGIN);

    assertNotNull(result);
    assertEquals(response, result);

    verify(profileClient).getProfileCode(argThat(r -> S21LOGIN.equals(r.getS21login())));
  }

  @Test
  void getVerificationCodeFeignException() {
    FeignException feignException = createFeignException(500, "Internal Server Error");

    when(profileClient.getProfileCode(any(ProfileCodeRequest.class))).thenThrow(feignException);

    assertThrows(FeignException.class, () -> profileService.getVerificationCode(S21LOGIN));
    verify(profileClient).getProfileCode(any(ProfileCodeRequest.class));
  }

  @Test
  void sendVerificationCodeSuccess() {
    ProfileCodeResponse codeResponse =
        new ProfileCodeResponse(S21LOGIN, SECRET_CODE, OffsetDateTime.now());
    RocketChatSendResponse rocketChatResponse = new RocketChatSendResponse(true, "ok");

    when(profileClient.getProfileCode(any(ProfileCodeRequest.class))).thenReturn(codeResponse);
    when(rocketChatClient.sendMessage(any(RocketChatSendRequest.class)))
        .thenReturn(rocketChatResponse);

    RocketChatSendResponse result = profileService.sendVerificationCode(S21LOGIN);

    assertNotNull(result);
    assertEquals(rocketChatResponse, result);

    verify(rocketChatClient)
        .sendMessage(
            argThat(
                req ->
                    S21LOGIN.equals(req.getUsername())
                        && req.getMessage() != null
                        && req.getMessage().contains(SECRET_CODE)));
  }

  @Test
  void sendVerificationCodeFailedResponseThrowsRocketChatSendException() {
    ProfileCodeResponse codeResponse =
        new ProfileCodeResponse(S21LOGIN, SECRET_CODE, OffsetDateTime.now());
    RocketChatSendResponse rocketChatResponse = new RocketChatSendResponse(false, "fail");

    when(profileClient.getProfileCode(any(ProfileCodeRequest.class))).thenReturn(codeResponse);
    when(rocketChatClient.sendMessage(any(RocketChatSendRequest.class)))
        .thenReturn(rocketChatResponse);

    RocketChatSendException ex =
        assertThrows(
            RocketChatSendException.class, () -> profileService.sendVerificationCode(S21LOGIN));

    assertEquals(rocketChatResponse, ex.getResponse());
  }

  @Test
  void sendVerificationCodeFeignException() {
    FeignException feignException = createFeignException(500, "Internal Server Error");

    when(profileClient.getProfileCode(any(ProfileCodeRequest.class))).thenThrow(feignException);

    assertThrows(FeignException.class, () -> profileService.sendVerificationCode(S21LOGIN));
    verify(profileClient).getProfileCode(any(ProfileCodeRequest.class));
    verify(rocketChatClient, never()).sendMessage(any(RocketChatSendRequest.class));
  }

  @Test
  void showCampusMapSuccess() {
    CampusResponse expectedResponse = createTestCampusResponse();

    when(profileClient.getCampusMap(any(CampusRequest.class))).thenReturn(expectedResponse);

    CampusResponse result = profileService.showCampusMap(CHAT_ID);

    assertNotNull(result);
    assertEquals(expectedResponse, result);
    verify(profileClient)
        .getCampusMap(argThat(request -> request.getTelegramId().equals(CHAT_ID.toString())));
  }

  @Test
  void showCampusMapFeignException() {
    FeignException feignException = createFeignException(500, "Internal Server Error");

    when(profileClient.getCampusMap(any(CampusRequest.class))).thenThrow(feignException);

    assertThrows(FeignException.class, () -> profileService.showCampusMap(CHAT_ID));
    verify(profileClient).getCampusMap(any(CampusRequest.class));
  }

  @Test
  void showParticipantSuccess() {
    ParticipantDto expectedParticipant = createTestParticipantDto();

    when(profileClient.getParticipant(any(ParticipantRequest.class)))
        .thenReturn(expectedParticipant);

    ParticipantDto result = profileService.showParticipant(CHAT_ID.toString(), LOGIN);

    assertNotNull(result);
    assertEquals(expectedParticipant, result);
    verify(profileClient)
        .getParticipant(
            argThat(
                request ->
                    request.getTelegramId().equals(CHAT_ID.toString())
                        && request.getEduLogin().equals(LOGIN)));
  }

  @Test
  void showParticipantFeignException() {
    FeignException feignException = createFeignException(500, "Internal Server Error");

    when(profileClient.getParticipant(any(ParticipantRequest.class))).thenThrow(feignException);

    String chatIdStr = CHAT_ID.toString();
    assertThrows(FeignException.class, () -> profileService.showParticipant(chatIdStr, LOGIN));
    verify(profileClient).getParticipant(any(ParticipantRequest.class));
  }

  @Test
  void setLastCommandSuccess() {
    Map<String, Object> args = new HashMap<>();
    LastCommandState command = new LastCommandState(LastCommandType.SEARCH, args);
    ProfileDto expectedProfile = createTestProfile();

    when(profileClient.setLastCommand(any(LastCommandRequest.class))).thenReturn(expectedProfile);

    profileService.setLastCommand(CHAT_ID, command);

    verify(profileClient)
        .setLastCommand(
            argThat(
                request ->
                    request.getTelegramId().equals(CHAT_ID.toString())
                        && request.getCommand().equals(command)));
  }

  @Test
  void applyFriendWithNameSuccess() {
    String name = "Test Friend";
    FriendRequest.Action action = FriendRequest.Action.SET_NAME;
    FriendDto expectedFriend = createTestFriendDto();

    when(profileClient.applyFriend(any(FriendRequest.class))).thenReturn(expectedFriend);

    FriendDto result = profileService.applyFriend(CHAT_ID, LOGIN, action, name);

    assertNotNull(result);
    assertEquals(expectedFriend, result);
    verify(profileClient)
        .applyFriend(
            argThat(
                request ->
                    request.getTelegramId().equals(CHAT_ID.toString())
                        && request.getLogin().equals(LOGIN)
                        && request.getAction() == action
                        && request.getName().equals(name)));
  }

  @Test
  void applyFriendWithoutNameSuccess() {
    FriendRequest.Action action = FriendRequest.Action.SET_NAME;
    FriendDto expectedFriend = createTestFriendDto();

    when(profileClient.applyFriend(any(FriendRequest.class))).thenReturn(expectedFriend);

    FriendDto result = profileService.applyFriend(CHAT_ID, LOGIN, action, null);

    assertNotNull(result);
    assertEquals(expectedFriend, result);
    verify(profileClient)
        .applyFriend(
            argThat(
                request ->
                    request.getTelegramId().equals(CHAT_ID.toString())
                        && request.getLogin().equals(LOGIN)
                        && request.getAction() == action
                        && request.getName() == null));
  }

  @Test
  void getFriendsSuccess() {
    int page = 0;
    int pageSize = 10;
    FriendsSliceDto expectedFriends = createTestFriendsSliceDto();

    when(profileClient.getFriends(CHAT_ID.toString(), page, pageSize)).thenReturn(expectedFriends);

    FriendsSliceDto result = profileService.getFriends(CHAT_ID, page, pageSize);

    assertNotNull(result);
    assertEquals(expectedFriends, result);
    verify(profileClient).getFriends(CHAT_ID.toString(), page, pageSize);
  }

  @Test
  void getEventSuccess() {
    long eventId = 123L;
    EventDto expectedEvent = createTestEventDto();

    when(profileClient.getEvent(eventId)).thenReturn(expectedEvent);

    EventDto result = profileService.getEvent(eventId);

    assertNotNull(result);
    assertEquals(expectedEvent, result);
    verify(profileClient).getEvent(eventId);
  }

  @Test
  void getEventsSuccess() {
    int page = 0;
    int pageSize = 10;
    EventsSliceDto expectedEvents = createTestEventsSliceDto();

    when(profileClient.getEvents(CHAT_ID.toString(), page, pageSize)).thenReturn(expectedEvents);

    EventsSliceDto result = profileService.getEvents(CHAT_ID, page, pageSize);

    assertNotNull(result);
    assertEquals(expectedEvents, result);
    verify(profileClient).getEvents(CHAT_ID.toString(), page, pageSize);
  }

  @Test
  void getProjectsSuccess() {
    List<ProjectsDto> expectedProjects = createTestProjectsList();

    when(profileClient.getProjects(LOGIN)).thenReturn(expectedProjects);

    List<ProjectsDto> result = profileService.getProjects(LOGIN);

    assertNotNull(result);
    assertEquals(expectedProjects, result);
    verify(profileClient).getProjects(LOGIN);
  }

  private ProfileDto createTestProfile() {
    Map<String, Object> args = new HashMap<>();
    LastCommandState lastCommand = new LastCommandState(LastCommandType.NONE, args);
    return new ProfileDto(CHAT_ID.toString(), LOGIN, ProfileStatus.CONFIRMED, lastCommand);
  }

  private CampusResponse createTestCampusResponse() {
    return new CampusResponse("Test Campus", Collections.emptyList(), Collections.emptyMap());
  }

  private ParticipantDto createTestParticipantDto() {
    return ParticipantDto.builder()
        .login(LOGIN)
        .className("Class A")
        .parallelName("Parallel 1")
        .expValue(100)
        .level(5)
        .expToNextLevel(50)
        .status(ParticipantStatusEnum.ACTIVE)
        .build();
  }

  private FriendDto createTestFriendDto() {
    return new FriendDto(
        CHAT_ID.toString(),
        LOGIN,
        "Test Friend",
        true,
        false,
        false,
        true,
        ParticipantStatusEnum.ACTIVE,
        "Cluster A",
        "Row 1",
        10,
        "Stage Group 1",
        "Stage 1");
  }

  private FriendsSliceDto createTestFriendsSliceDto() {
    return new FriendsSliceDto(Collections.singletonList(createTestFriendDto()), 0, 10, false);
  }

  private EventDto createTestEventDto() {
    return new EventDto(
        1L,
        "WORKSHOP",
        "Test Event",
        "Test Description",
        "Location 1",
        null,
        null,
        Collections.singletonList("Organizer 1"),
        50,
        10);
  }

  private EventsSliceDto createTestEventsSliceDto() {
    return new EventsSliceDto(Collections.singletonList(createTestEventDto()), 0, 10, false);
  }

  private List<ProjectsDto> createTestProjectsList() {
    ProjectsDto project =
        new ProjectsDto(
            "goal1",
            "Test Project",
            "Test Description",
            100,
            "2026-02-17T10:00:00Z",
            95,
            10,
            "INDIVIDUAL",
            "COMPLETED",
            2,
            1);
    return Collections.singletonList(project);
  }

  private FeignException createFeignException(int status, String message) {
    Request request =
        Request.create(
            Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
    Response response =
        Response.builder()
            .status(status)
            .request(request)
            .body(message, StandardCharsets.UTF_8)
            .build();
    return FeignException.errorStatus("Test", response);
  }
}
