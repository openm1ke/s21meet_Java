package ru.izpz.bot.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
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
import ru.izpz.dto.*;
import ru.izpz.dto.model.ErrorResponseDTO;
import ru.izpz.utils.FeignErrorParser;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileClient profileClient;

    @Mock
    private RocketChatClient rocketChatClient;

    @InjectMocks
    private ProfileService profileService;

    private final Long chatId = 12345L;
    private final String login = "test.login";
    private final String s21login = "s21.test.login";
    private final String secretCode = "123456";

    @BeforeEach
    void setUp() {
        // Empty setUp method - test initialization is handled by @ExtendWith(MockitoExtension.class)
        // and field injection with @Mock and @InjectMocks annotations
    }

    @Test
    void getProfile_Success() {
        ProfileDto expectedProfile = createTestProfile();
        when(profileClient.getOrCreateProfile(chatId.toString())).thenReturn(expectedProfile);

        ProfileDto result = profileService.getProfile(chatId);

        assertNotNull(result);
        assertEquals(expectedProfile, result);
        verify(profileClient).getOrCreateProfile(chatId.toString());
    }

    @Test
    void getProfile_FeignException() {
        FeignException feignException = createFeignException(500, "Internal Server Error");
        when(profileClient.getOrCreateProfile(chatId.toString())).thenThrow(feignException);

        assertThrows(FeignException.class, () -> profileService.getProfile(chatId));
        verify(profileClient).getOrCreateProfile(chatId.toString());
    }

    @Test
    void checkEduLogin_FeignException_ThrowsEduLoginCheckException() {
        FeignException feignException = createFeignException(400, "Bad Request");
        ErrorResponseDTO errorResponse = new ErrorResponseDTO()
                .status(400)
                .exceptionUUID("test-uuid")
                .code("login_NOT_FOUND")
                .message("Login not found");
        
        when(profileClient.checkEduLogin(login)).thenThrow(feignException);
        
        try (MockedStatic<FeignErrorParser> parserMock = mockStatic(FeignErrorParser.class)) {
            parserMock.when(() -> FeignErrorParser.parse(feignException))
                    .thenReturn(errorResponse);

            EduLoginCheckException exception = assertThrows(EduLoginCheckException.class,
                    () -> profileService.checkEduLogin(login));

            assertEquals(errorResponse, exception.getError());
            verify(profileClient).checkEduLogin(login);
        }
    }

    @Test
    void updateProfileStatus_Success() {
        ProfileStatus status = ProfileStatus.CONFIRMED;
        ProfileDto expectedProfile = createTestProfile();
        
        when(profileClient.updateProfileStatus(any(ProfileRequest.class)))
                .thenReturn(expectedProfile);

        ProfileDto result = profileService.updateProfileStatus(chatId, status);

        assertNotNull(result);
        assertEquals(expectedProfile, result);
        verify(profileClient).updateProfileStatus(argThat(request ->
                request.getTelegramId().equals(chatId.toString()) &&
                request.getStatus() == status
        ));
    }

    @Test
    void updateProfileStatus_FeignException() {
        ProfileStatus status = ProfileStatus.CONFIRMED;
        FeignException feignException = createFeignException(500, "Internal Server Error");
        
        when(profileClient.updateProfileStatus(any(ProfileRequest.class)))
                .thenThrow(feignException);

        assertThrows(FeignException.class, () -> profileService.updateProfileStatus(chatId, status));
        verify(profileClient).updateProfileStatus(any(ProfileRequest.class));
    }

    @Test
    void checkAndSetLogin_Success() {
        ProfileDto expectedProfile = createTestProfile();
        
        when(profileClient.checkAndSetLogin(any(ProfileRequest.class)))
                .thenReturn(expectedProfile);

        ProfileDto result = profileService.checkAndSetLogin(chatId, login);

        assertNotNull(result);
        assertEquals(expectedProfile, result);
        verify(profileClient).checkAndSetLogin(argThat(request ->
                request.getTelegramId().equals(chatId.toString()) &&
                request.getS21login().equals(login)
        ));
    }

    @Test
    void checkAndSetLogin_FeignException() {
        FeignException feignException = createFeignException(500, "Internal Server Error");
        
        when(profileClient.checkAndSetLogin(any(ProfileRequest.class)))
                .thenThrow(feignException);

        assertThrows(FeignException.class, () -> profileService.checkAndSetLogin(chatId, login));
        verify(profileClient).checkAndSetLogin(any(ProfileRequest.class));
    }

    @Test
    void getVerificationCode_Success() {
        ProfileCodeResponse response = new ProfileCodeResponse(s21login, secretCode, OffsetDateTime.now());
        when(profileClient.getProfileCode(any(ProfileCodeRequest.class))).thenReturn(response);

        ProfileCodeResponse result = profileService.getVerificationCode(s21login);

        assertNotNull(result);
        assertEquals(response, result);

        verify(profileClient).getProfileCode(argThat(r -> s21login.equals(r.getS21login())));
    }

    @Test
    void getVerificationCode_FeignException() {
        FeignException feignException = createFeignException(500, "Internal Server Error");
        
        when(profileClient.getProfileCode(any(ProfileCodeRequest.class)))
                .thenThrow(feignException);

        assertThrows(FeignException.class, () -> profileService.getVerificationCode(s21login));
        verify(profileClient).getProfileCode(any(ProfileCodeRequest.class));
    }

    @Test
    void sendVerificationCode_Success() {
        ProfileCodeResponse codeResponse = new ProfileCodeResponse(s21login, secretCode, OffsetDateTime.now());
        RocketChatSendResponse rocketChatResponse = new RocketChatSendResponse(true, "ok");

        when(profileClient.getProfileCode(any(ProfileCodeRequest.class))).thenReturn(codeResponse);
        when(rocketChatClient.sendMessage(any(RocketChatSendRequest.class))).thenReturn(rocketChatResponse);

        RocketChatSendResponse result = profileService.sendVerificationCode(s21login);

        assertNotNull(result);
        assertEquals(rocketChatResponse, result);

        verify(rocketChatClient).sendMessage(argThat(req ->
                s21login.equals(req.getUsername()) &&
                req.getMessage() != null &&
                req.getMessage().contains(secretCode)
        ));
    }

    @Test
    void sendVerificationCode_FailedResponse_ThrowsRocketChatSendException() {
        ProfileCodeResponse codeResponse = new ProfileCodeResponse(s21login, secretCode, OffsetDateTime.now());
        RocketChatSendResponse rocketChatResponse = new RocketChatSendResponse(false, "fail");

        when(profileClient.getProfileCode(any(ProfileCodeRequest.class))).thenReturn(codeResponse);
        when(rocketChatClient.sendMessage(any(RocketChatSendRequest.class))).thenReturn(rocketChatResponse);

        RocketChatSendException ex = assertThrows(RocketChatSendException.class,
                () -> profileService.sendVerificationCode(s21login));

        assertEquals(rocketChatResponse, ex.getResponse());
    }

    @Test
    void sendVerificationCode_FeignException() {
        FeignException feignException = createFeignException(500, "Internal Server Error");
        
        when(profileClient.getProfileCode(any(ProfileCodeRequest.class)))
                .thenThrow(feignException);

        assertThrows(FeignException.class, () -> profileService.sendVerificationCode(s21login));
        verify(profileClient).getProfileCode(any(ProfileCodeRequest.class));
        verify(rocketChatClient, never()).sendMessage(any(RocketChatSendRequest.class));
    }

    @Test
    void showCampusMap_Success() {
        CampusResponse expectedResponse = createTestCampusResponse();
        
        when(profileClient.getCampusMap(any(CampusRequest.class)))
                .thenReturn(expectedResponse);

        CampusResponse result = profileService.showCampusMap(chatId);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(profileClient).getCampusMap(argThat(request ->
                request.getTelegramId().equals(chatId.toString())
        ));
    }

    @Test
    void showCampusMap_FeignException() {
        FeignException feignException = createFeignException(500, "Internal Server Error");
        
        when(profileClient.getCampusMap(any(CampusRequest.class)))
                .thenThrow(feignException);

        assertThrows(FeignException.class, () -> profileService.showCampusMap(chatId));
        verify(profileClient).getCampusMap(any(CampusRequest.class));
    }

    @Test
    void showParticipant_Success() {
        ParticipantDto expectedParticipant = createTestParticipantDto();
        
        when(profileClient.getParticipant(any(ParticipantRequest.class)))
                .thenReturn(expectedParticipant);

        ParticipantDto result = profileService.showParticipant(chatId.toString(), login);

        assertNotNull(result);
        assertEquals(expectedParticipant, result);
        verify(profileClient).getParticipant(argThat(request ->
                request.getTelegramId().equals(chatId.toString()) &&
                request.getEduLogin().equals(login)
        ));
    }

    @Test
    void showParticipant_FeignException() {
        FeignException feignException = createFeignException(500, "Internal Server Error");
        
        when(profileClient.getParticipant(any(ParticipantRequest.class)))
                .thenThrow(feignException);

        assertThrows(FeignException.class, () -> profileService.showParticipant(chatId.toString(), login));
        verify(profileClient).getParticipant(any(ParticipantRequest.class));
    }

    @Test
    void setLastCommand_Success() {
        Map<String, Object> args = new HashMap<>();
        LastCommandState command = new LastCommandState(LastCommandType.SEARCH, args);
        ProfileDto expectedProfile = createTestProfile();
        
        when(profileClient.setLastCommand(any(LastCommandRequest.class)))
                .thenReturn(expectedProfile);

        profileService.setLastCommand(chatId, command);

        verify(profileClient).setLastCommand(argThat(request ->
                request.getTelegramId().equals(chatId.toString()) &&
                request.getCommand().equals(command)
        ));
    }

    @Test
    void applyFriend_WithName_Success() {
        String name = "Test Friend";
        FriendRequest.Action action = FriendRequest.Action.SET_NAME;
        FriendDto expectedFriend = createTestFriendDto();
        
        when(profileClient.applyFriend(any(FriendRequest.class)))
                .thenReturn(expectedFriend);

        FriendDto result = profileService.applyFriend(chatId, login, action, name);

        assertNotNull(result);
        assertEquals(expectedFriend, result);
        verify(profileClient).applyFriend(argThat(request ->
                request.getTelegramId().equals(chatId.toString()) &&
                request.getLogin().equals(login) &&
                request.getAction() == action &&
                request.getName().equals(name)
        ));
    }

    @Test
    void applyFriend_WithoutName_Success() {
        FriendRequest.Action action = FriendRequest.Action.SET_NAME;
        FriendDto expectedFriend = createTestFriendDto();
        
        when(profileClient.applyFriend(any(FriendRequest.class)))
                .thenReturn(expectedFriend);

        FriendDto result = profileService.applyFriend(chatId, login, action, null);

        assertNotNull(result);
        assertEquals(expectedFriend, result);
        verify(profileClient).applyFriend(argThat(request ->
                request.getTelegramId().equals(chatId.toString()) &&
                request.getLogin().equals(login) &&
                request.getAction() == action &&
                request.getName() == null
        ));
    }

    @Test
    void getFriends_Success() {
        int page = 0;
        int pageSize = 10;
        FriendsSliceDto expectedFriends = createTestFriendsSliceDto();
        
        when(profileClient.getFriends(chatId.toString(), page, pageSize))
                .thenReturn(expectedFriends);

        FriendsSliceDto result = profileService.getFriends(chatId, page, pageSize);

        assertNotNull(result);
        assertEquals(expectedFriends, result);
        verify(profileClient).getFriends(chatId.toString(), page, pageSize);
    }

    @Test
    void getEvent_Success() {
        long eventId = 123L;
        EventDto expectedEvent = createTestEventDto();
        
        when(profileClient.getEvent(eventId))
                .thenReturn(expectedEvent);

        EventDto result = profileService.getEvent(eventId);

        assertNotNull(result);
        assertEquals(expectedEvent, result);
        verify(profileClient).getEvent(eventId);
    }

    @Test
    void getEvents_Success() {
        int page = 0;
        int pageSize = 10;
        EventsSliceDto expectedEvents = createTestEventsSliceDto();
        
        when(profileClient.getEvents(chatId.toString(), page, pageSize))
                .thenReturn(expectedEvents);

        EventsSliceDto result = profileService.getEvents(chatId, page, pageSize);

        assertNotNull(result);
        assertEquals(expectedEvents, result);
        verify(profileClient).getEvents(chatId.toString(), page, pageSize);
    }

    @Test
    void getProjects_Success() {
        List<ProjectsDto> expectedProjects = createTestProjectsList();
        
        when(profileClient.getProjects(login))
                .thenReturn(expectedProjects);

        List<ProjectsDto> result = profileService.getProjects(login);

        assertNotNull(result);
        assertEquals(expectedProjects, result);
        verify(profileClient).getProjects(login);
    }

    private ProfileDto createTestProfile() {
        Map<String, Object> args = new HashMap<>();
        LastCommandState lastCommand = new LastCommandState(LastCommandType.NONE, args);
        return new ProfileDto(
                chatId.toString(),
                login,
                ProfileStatus.CONFIRMED,
                lastCommand
        );
    }

    private CampusResponse createTestCampusResponse() {
        return new CampusResponse(
                "Test Campus",
                Collections.emptyList()
        );
    }

    private ParticipantDto createTestParticipantDto() {
        return new ParticipantDto(
                login,
                "Class A",
                "Parallel 1",
                100,
                5,
                50,
                ParticipantStatusEnum.ACTIVE,
                null
        );
    }

    private FriendDto createTestFriendDto() {
        return new FriendDto(
                chatId.toString(),
                login,
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
                "Stage 1"
        );
    }

    private FriendsSliceDto createTestFriendsSliceDto() {
        return new FriendsSliceDto(
                Collections.singletonList(createTestFriendDto()),
                0,
                10,
                false
        );
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
                10
        );
    }

    private EventsSliceDto createTestEventsSliceDto() {
        return new EventsSliceDto(
                Collections.singletonList(createTestEventDto()),
                0,
                10,
                false
        );
    }

    private List<ProjectsDto> createTestProjectsList() {
        ProjectsDto project = new ProjectsDto(
                "goal1",
                "Test Project",
                "Test Description",
                100,
                "2026-02-17T10:00:00Z",
                95,
                10,
                "INDIVIDUAL",
                "COMPLETED",
                "BASIC",
                "PASSED",
                5,
                2,
                2,
                3,
                2,
                1,
                "Group A",
                1
        );
        return Collections.singletonList(project);
    }

    private FeignException createFeignException(int status, String message) {
        Request request = Request.create(Request.HttpMethod.GET, "/test", 
                Collections.emptyMap(), null, new RequestTemplate());
        Response response = Response.builder()
                .status(status)
                .request(request)
                .body(message, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("Test", response);
    }
}
