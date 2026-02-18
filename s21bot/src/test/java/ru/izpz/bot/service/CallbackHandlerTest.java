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
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.InvalidCallbackPayloadException;
import ru.izpz.bot.keyboard.CallbackPayloadSerializer;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.*;
import ru.izpz.dto.model.ErrorResponseDTO;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackHandlerTest {

    @Mock
    private BotProperties botProperties;

    @Mock
    private ProfileService profileService;

    @Mock
    private TelegramKeyboardFactory telegramKeyboardFactory;

    @Mock
    private CallbackPayloadSerializer callbackPayloadSerializer;

    @Mock
    private MessageSender messageSender;

    @InjectMocks
    private CallbackHandler callbackHandler;

    private final Long CHAT_ID = 10L;

    @BeforeEach
    void setUp() {
        lenient().when(botProperties.admin()).thenReturn(999L);
    }

    @Test
    void handleCallbackMessage_invalidPayload_sendsErrorMessage() {
        when(callbackPayloadSerializer.deserialize("bad")).thenThrow(new InvalidCallbackPayloadException("x", null));

        callbackHandler.handleCallbackMessage(CHAT_ID, "bad", 1, "cb");

        verify(messageSender).sendMessage(CHAT_ID, "Некорректный формат данных. Попробуйте еще раз.", null);
    }

    @Test
    void handleCallbackMessage_friendsPage_callsShowFriends() {
        when(callbackPayloadSerializer.deserialize("data")).thenReturn(new CallbackPayload("friends_page", Map.of("page", "2")));

        FriendsSliceDto slice = new FriendsSliceDto(java.util.Collections.emptyList(), 2, 2, false);
        when(profileService.getFriends(CHAT_ID, 2, 2)).thenReturn(slice);
        when(telegramKeyboardFactory.friendsListKeyboard(eq(slice), anyInt(), eq(2))).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.friendsListText(eq(slice))).thenReturn("text");

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 5, "cb");

        verify(messageSender).updateMessage(eq(CHAT_ID), eq(5), eq("text"), any());
    }

    @Test
    void handleCallbackMessage_eventsPage_callsShowEvents() {
        when(callbackPayloadSerializer.deserialize("data")).thenReturn(new CallbackPayload("events_page", Map.of("page", "1")));

        EventsSliceDto slice = new EventsSliceDto(java.util.Collections.emptyList(), 1, 2, false);
        when(profileService.getEvents(CHAT_ID, 1, 2)).thenReturn(slice);
        when(telegramKeyboardFactory.eventsListKeyboard(eq(slice), anyInt(), eq(1))).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.eventsListText(eq(slice))).thenReturn("events");

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 5, "cb");

        verify(messageSender).updateMessage(eq(CHAT_ID), eq(5), eq("events"), any());
    }

    @Test
    void handleCallbackMessage_showFriend_callsShowProfile() {
        when(callbackPayloadSerializer.deserialize("data")).thenReturn(new CallbackPayload("show_friend", Map.of("login", "abc")));

        when(profileService.checkEduLogin("abc")).thenReturn(new ru.izpz.dto.model.ParticipantV1DTO());
        FriendDto friend = FriendDto.builder().login("abc").isFriend(false).build();
        when(profileService.applyFriend(CHAT_ID, "abc", FriendRequest.Action.NONE, null)).thenReturn(friend);

        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.getFriendInlineKeyboard("abc", friend)).thenReturn(kb);

        ParticipantDto participant = mock(ParticipantDto.class);
        when(profileService.showParticipant(CHAT_ID.toString(), "abc")).thenReturn(participant);

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 5, "cb");

        verify(messageSender).sendMessage(eq(CHAT_ID), startsWith("Профиль\n"), eq(kb));
    }

    @Test
    void showEvents_feignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        when(profileService.getEvents(CHAT_ID, 0, 2)).thenThrow(ex);

        callbackHandler.showEvents(CHAT_ID, 0, null);

        verify(messageSender).sendMessage(CHAT_ID, "Ошибка обработки событий, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void showFriends_feignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        when(profileService.getFriends(CHAT_ID, 0, 2)).thenThrow(ex);

        callbackHandler.showFriends(CHAT_ID, 0, null);

        verify(messageSender).sendMessage(CHAT_ID, "Ошибка обработки друзей, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void handleCallbackMessage_registration_updatesMessageAndSetsStatusRegistration() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload(ru.izpz.bot.keyboard.TelegramButtons.REGISTRATION_CODE, null));

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 5, "cb");

        verify(messageSender).updateMessage(CHAT_ID, 5, "Введите логин на платформе", null);
        verify(profileService).updateProfileStatus(CHAT_ID, ProfileStatus.REGISTRATION);
    }

    @Test
    void handleCallbackMessage_addFriend_appliesFriendAndRefreshesKeyboardAndToasts() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("add_friend", Map.of("login", "abc")));

        FriendDto friend = FriendDto.builder().login("abc").isFriend(true).build();
        when(profileService.applyFriend(CHAT_ID, "abc", FriendRequest.Action.TOGGLE_FRIEND, null)).thenReturn(friend);

        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.getFriendInlineKeyboard("abc", friend)).thenReturn(kb);

        EditMessageReplyMarkup edit = mock(EditMessageReplyMarkup.class);
        when(telegramKeyboardFactory.editFriendInlineKeyboard(kb, CHAT_ID, 7)).thenReturn(edit);

        var answer = mock(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.class);
        when(telegramKeyboardFactory.createAnswerCallbackQuery("cb", "Статус «друг» переключён", false)).thenReturn(answer);

        when(messageSender.execute(any())).thenReturn(java.util.Optional.empty());

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 7, "cb");

        verify(profileService).applyFriend(CHAT_ID, "abc", FriendRequest.Action.TOGGLE_FRIEND, null);
        verify(messageSender, times(2)).execute(any());
    }

    @Test
    void handleCallbackMessage_favorite_appliesFriendToggleFavorite() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("favorite", Map.of("login", "abc")));

        FriendDto friend = FriendDto.builder().login("abc").isFriend(true).build();
        when(profileService.applyFriend(CHAT_ID, "abc", FriendRequest.Action.TOGGLE_FAVORITE, null)).thenReturn(friend);
        when(telegramKeyboardFactory.getFriendInlineKeyboard(eq("abc"), eq(friend))).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.editFriendInlineKeyboard(any(), eq(CHAT_ID), eq(7))).thenReturn(mock(EditMessageReplyMarkup.class));
        when(telegramKeyboardFactory.createAnswerCallbackQuery(eq("cb"), anyString(), eq(false)))
                .thenReturn(mock(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.class));
        when(messageSender.execute(any())).thenReturn(java.util.Optional.empty());

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 7, "cb");

        verify(profileService).applyFriend(CHAT_ID, "abc", FriendRequest.Action.TOGGLE_FAVORITE, null);
    }

    @Test
    void handleCallbackMessage_subscribe_appliesFriendToggleSubscribe() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("subscribe", Map.of("login", "abc")));

        FriendDto friend = FriendDto.builder().login("abc").isFriend(true).build();
        when(profileService.applyFriend(CHAT_ID, "abc", FriendRequest.Action.TOGGLE_SUBSCRIBE, null)).thenReturn(friend);
        when(telegramKeyboardFactory.getFriendInlineKeyboard(eq("abc"), eq(friend))).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.editFriendInlineKeyboard(any(), eq(CHAT_ID), eq(7))).thenReturn(mock(EditMessageReplyMarkup.class));
        when(telegramKeyboardFactory.createAnswerCallbackQuery(eq("cb"), anyString(), eq(false)))
                .thenReturn(mock(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.class));
        when(messageSender.execute(any())).thenReturn(java.util.Optional.empty());

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 7, "cb");

        verify(profileService).applyFriend(CHAT_ID, "abc", FriendRequest.Action.TOGGLE_SUBSCRIBE, null);
    }

    @Test
    void showFriends_success_messageIdNull_sendsMessage() {
        FriendsSliceDto slice = new FriendsSliceDto(java.util.Collections.emptyList(), 0, 2, false);
        when(profileService.getFriends(CHAT_ID, 0, 2)).thenReturn(slice);
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.friendsListKeyboard(eq(slice), anyInt(), eq(0))).thenReturn(kb);
        when(telegramKeyboardFactory.friendsListText(eq(slice))).thenReturn("friends");

        callbackHandler.showFriends(CHAT_ID, 0, null);

        verify(messageSender).sendMessage(CHAT_ID, "friends", kb);
    }

    @Test
    void showEvents_success_messageIdNull_sendsMessageWithTitle() {
        EventsSliceDto slice = new EventsSliceDto(java.util.Collections.emptyList(), 0, 2, false);
        when(profileService.getEvents(CHAT_ID, 0, 2)).thenReturn(slice);
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.eventsListKeyboard(eq(slice), anyInt(), eq(0))).thenReturn(kb);
        when(telegramKeyboardFactory.eventsListText(eq(slice))).thenReturn("events");

        callbackHandler.showEvents(CHAT_ID, 0, null);

        verify(messageSender).sendMessage(CHAT_ID, "События\n\nevents", kb);
    }

    @Test
    void showProfile_success_sendsProfileWithKeyboard() {
        when(profileService.checkEduLogin("abc")).thenReturn(new ru.izpz.dto.model.ParticipantV1DTO());
        FriendDto friend = FriendDto.builder().login("abc").isFriend(false).build();
        when(profileService.applyFriend(CHAT_ID, "abc", FriendRequest.Action.NONE, null)).thenReturn(friend);
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.getFriendInlineKeyboard("abc", friend)).thenReturn(kb);
        ParticipantDto participant = mock(ParticipantDto.class);
        when(profileService.showParticipant(CHAT_ID.toString(), "abc")).thenReturn(participant);

        callbackHandler.showProfile(CHAT_ID, "abc");

        verify(messageSender).sendMessage(eq(CHAT_ID), startsWith("Профиль\n"), eq(kb));
    }

    @Test
    void handleCallbackMessage_setName_setsLastCommandAndAsksForName() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("set_name", Map.of("login", "abc")));

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 1, "cb");

        verify(profileService).setLastCommand(eq(CHAT_ID), any(LastCommandState.class));
        verify(messageSender).sendMessage(CHAT_ID, "Указать имя", null);
    }

    @Test
    void handleCallbackMessage_event_sendsEventDto() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("event", Map.of("id", "1")));

        EventDto event = new EventDto(1L, "t", "n", null, null, null, null, java.util.List.of(), null, null);
        when(profileService.getEvent(1L)).thenReturn(event);

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 1, "cb");

        verify(messageSender).sendMessage(CHAT_ID, event.toString(), null);
    }

    @Test
    void handleCallbackMessage_unknown_sendsUnknownCommandMessage() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("unknown", null));

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 1, "cb");

        verify(messageSender).sendMessage(CHAT_ID, "Неизвестная команда: data", null);
    }

    @Test
    void showProfile_invalidLogin_sendsValidationMessage() {
        callbackHandler.showProfile(CHAT_ID, "12");

        verify(messageSender).sendMessage(CHAT_ID, "Логин должен быть от 3 до 30 символов и состоять только из латинских букв", null);
        verifyNoInteractions(profileService);
    }

    @Test
    void showProfile_nullLogin_sendsValidationMessage() {
        callbackHandler.showProfile(CHAT_ID, null);

        verify(messageSender).sendMessage(CHAT_ID, "Логин должен быть от 3 до 30 символов и состоять только из латинских букв", null);
        verifyNoInteractions(profileService);
    }

    @Test
    void showProfile_whenFeignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        when(profileService.checkEduLogin("abc")).thenThrow(ex);

        callbackHandler.showProfile(CHAT_ID, "abc");

        verify(messageSender).sendMessage(CHAT_ID, "Ошибка поиска профиля, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void showProfile_whenEduLoginCheckException_sendsUserAndAdmin() {
        ErrorResponseDTO err = new ErrorResponseDTO().status(400).message("bad");
        when(profileService.checkEduLogin("abc")).thenThrow(new EduLoginCheckException(err));

        callbackHandler.showProfile(CHAT_ID, "abc");

        verify(messageSender).sendMessage(CHAT_ID, "Ошибка проверки логина: bad", null);
        verify(messageSender).sendMessage(999L, "Ошибка проверки логина: " + err, null);
    }

    @Test
    void setLastCommand_whenFeignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        doThrow(ex).when(profileService).setLastCommand(eq(CHAT_ID), any());

        callbackHandler.setLastCommand(CHAT_ID, LastCommandType.SEARCH, null);

        verify(messageSender).sendMessage(CHAT_ID, "Ошибка установки lastCommand", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void updateMessageAndChangeStatusRegistration_whenFeignException_doesNotThrow() {
        doThrow(createFeignException(500, "boom")).when(profileService).updateProfileStatus(CHAT_ID, ProfileStatus.REGISTRATION);

        callbackHandler.updateMessageAndChangeStatusRegistration(CHAT_ID, 1, "t");

        verify(messageSender).updateMessage(CHAT_ID, 1, "t", null);
    }

    private FeignException createFeignException(int status, String message) {
        Request request = Request.create(Request.HttpMethod.GET, "/test",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        Response response = Response.builder()
                .status(status)
                .request(request)
                .body(message, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("Test", response);
    }
}
