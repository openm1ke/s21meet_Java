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
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.*;
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

    private final Long chatId = 10L;

    @BeforeEach
    void setUp() {
        lenient().when(botProperties.admin()).thenReturn(999L);
    }

    @Test
    void handleCallbackMessage_invalidPayload_sendsErrorMessage() {
        when(callbackPayloadSerializer.deserialize("bad")).thenThrow(new InvalidCallbackPayloadException("x", null));

        callbackHandler.handleCallbackMessage(chatId, "bad", 1, "cb");

        verify(messageSender).sendMessage(chatId, "Некорректный формат данных. Попробуйте еще раз.", null);
    }

    @Test
    void handleCallbackMessage_friendsPage_callsShowFriends() {
        when(callbackPayloadSerializer.deserialize("data")).thenReturn(new CallbackPayload("friends_page", Map.of("page", "2")));

        FriendsSliceDto slice = new FriendsSliceDto(java.util.Collections.emptyList(), 2, 2, false);
        when(profileService.getFriends(chatId, 2, 2)).thenReturn(slice);
        when(telegramKeyboardFactory.friendsListKeyboard(eq(slice), anyInt(), eq(2))).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.friendsListText(slice)).thenReturn("text");

        callbackHandler.handleCallbackMessage(chatId, "data", 5, "cb");

        verify(messageSender).updateMessage(eq(chatId), eq(5), eq("text"), ArgumentMatchers.any());
    }

    @Test
    void handleCallbackMessage_eventsPage_callsShowEvents() {
        when(callbackPayloadSerializer.deserialize("data")).thenReturn(new CallbackPayload("events_page", Map.of("page", "1")));

        EventsSliceDto slice = new EventsSliceDto(java.util.Collections.emptyList(), 1, 2, false);
        when(profileService.getEvents(chatId, 1, 2)).thenReturn(slice);
        when(telegramKeyboardFactory.eventsListKeyboard(eq(slice), anyInt(), eq(1))).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.eventsListText(slice)).thenReturn("events");

        callbackHandler.handleCallbackMessage(chatId, "data", 5, "cb");

        verify(messageSender).updateMessage(eq(chatId), eq(5), eq("events"), ArgumentMatchers.any());
    }

    @Test
    void handleCallbackMessage_showFriend_callsShowProfile() {
        when(callbackPayloadSerializer.deserialize("data")).thenReturn(new CallbackPayload("show_friend", Map.of("login", "abc")));

        when(profileService.checkEduLogin("abc")).thenReturn(new ru.izpz.dto.model.ParticipantV1DTO());
        FriendDto friend = FriendDto.builder().login("abc").isFriend(false).build();
        when(profileService.applyFriend(chatId, "abc", FriendRequest.Action.NONE, null)).thenReturn(friend);

        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.getFriendInlineKeyboard("abc", friend)).thenReturn(kb);

        ParticipantDto participant = mock(ParticipantDto.class);
        when(profileService.showParticipant(chatId.toString(), "abc")).thenReturn(participant);

        callbackHandler.handleCallbackMessage(chatId, "data", 5, "cb");

        verify(messageSender).sendMessage(eq(chatId), startsWith("Профиль\n"), eq(kb));
    }

    @Test
    void showEvents_feignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        when(profileService.getEvents(chatId, 0, 2)).thenThrow(ex);

        callbackHandler.showEvents(chatId, 0, null);

        verify(messageSender).sendMessage(chatId, "Ошибка обработки событий, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void showFriends_feignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        when(profileService.getFriends(chatId, 0, 2)).thenThrow(ex);

        callbackHandler.showFriends(chatId, 0, null);

        verify(messageSender).sendMessage(chatId, "Ошибка обработки друзей, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void handleCallbackMessage_registration_updatesMessageAndSetsStatusRegistration() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload(ru.izpz.bot.keyboard.TelegramButtons.REGISTRATION_CODE, null));

        callbackHandler.handleCallbackMessage(chatId, "data", 5, "cb");

        verify(messageSender).updateMessage(chatId, 5, "Введите логин на платформе", null);
        verify(profileService).updateProfileStatus(chatId, ProfileStatus.REGISTRATION);
    }

    @Test
    void handleCallbackMessage_addFriend_appliesFriendAndRefreshesKeyboardAndToasts() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("add_friend", Map.of("login", "abc")));

        FriendDto friend = FriendDto.builder().login("abc").isFriend(true).build();
        when(profileService.applyFriend(chatId, "abc", FriendRequest.Action.TOGGLE_FRIEND, null)).thenReturn(friend);

        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.getFriendInlineKeyboard("abc", friend)).thenReturn(kb);

        EditMessageReplyMarkup edit = mock(EditMessageReplyMarkup.class);
        when(telegramKeyboardFactory.editFriendInlineKeyboard(kb, chatId, 7)).thenReturn(edit);

        var answer = mock(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.class);
        when(telegramKeyboardFactory.createAnswerCallbackQuery("cb", "Статус «друг» переключён", false)).thenReturn(answer);

        when(messageSender.execute(ArgumentMatchers.any())).thenReturn(java.util.Optional.empty());

        callbackHandler.handleCallbackMessage(chatId, "data", 7, "cb");

        verify(profileService).applyFriend(chatId, "abc", FriendRequest.Action.TOGGLE_FRIEND, null);
        verify(messageSender, times(2)).execute(ArgumentMatchers.any());
    }

    @Test
    void handleCallbackMessage_favorite_appliesFriendToggleFavorite() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("favorite", Map.of("login", "abc")));

        FriendDto friend = FriendDto.builder().login("abc").isFriend(true).build();
        when(profileService.applyFriend(chatId, "abc", FriendRequest.Action.TOGGLE_FAVORITE, null)).thenReturn(friend);
        when(telegramKeyboardFactory.getFriendInlineKeyboard("abc", friend)).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.editFriendInlineKeyboard(ArgumentMatchers.any(), eq(chatId), eq(7))).thenReturn(mock(EditMessageReplyMarkup.class));
        when(telegramKeyboardFactory.createAnswerCallbackQuery(eq("cb"), anyString(), eq(false)))
                .thenReturn(mock(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.class));
        when(messageSender.execute(ArgumentMatchers.any())).thenReturn(java.util.Optional.empty());

        callbackHandler.handleCallbackMessage(chatId, "data", 7, "cb");

        verify(profileService).applyFriend(chatId, "abc", FriendRequest.Action.TOGGLE_FAVORITE, null);
    }

    @Test
    void handleCallbackMessage_subscribe_appliesFriendToggleSubscribe() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("subscribe", Map.of("login", "abc")));

        FriendDto friend = FriendDto.builder().login("abc").isFriend(true).build();
        when(profileService.applyFriend(chatId, "abc", FriendRequest.Action.TOGGLE_SUBSCRIBE, null)).thenReturn(friend);
        when(telegramKeyboardFactory.getFriendInlineKeyboard(eq("abc"), eq(friend))).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.editFriendInlineKeyboard(ArgumentMatchers.any(), eq(chatId), eq(7))).thenReturn(mock(EditMessageReplyMarkup.class));
        when(telegramKeyboardFactory.createAnswerCallbackQuery(eq("cb"), anyString(), eq(false)))
                .thenReturn(mock(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.class));
        when(messageSender.execute(ArgumentMatchers.any())).thenReturn(java.util.Optional.empty());

        callbackHandler.handleCallbackMessage(chatId, "data", 7, "cb");

        verify(profileService).applyFriend(chatId, "abc", FriendRequest.Action.TOGGLE_SUBSCRIBE, null);
    }

    @Test
    void showFriends_success_messageIdNull_sendsMessage() {
        FriendsSliceDto slice = new FriendsSliceDto(java.util.Collections.emptyList(), 0, 2, false);
        when(profileService.getFriends(chatId, 0, 2)).thenReturn(slice);
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.friendsListKeyboard(eq(slice), anyInt(), eq(0))).thenReturn(kb);
        when(telegramKeyboardFactory.friendsListText(eq(slice))).thenReturn("friends");

        callbackHandler.showFriends(chatId, 0, null);

        verify(messageSender).sendMessage(chatId, "friends", kb);
    }

    @Test
    void showEvents_success_messageIdNull_sendsMessageWithTitle() {
        EventsSliceDto slice = new EventsSliceDto(java.util.Collections.emptyList(), 0, 2, false);
        when(profileService.getEvents(chatId, 0, 2)).thenReturn(slice);
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.eventsListKeyboard(eq(slice), anyInt(), eq(0))).thenReturn(kb);
        when(telegramKeyboardFactory.eventsListText(eq(slice))).thenReturn("events");

        callbackHandler.showEvents(chatId, 0, null);

        verify(messageSender).sendMessage(chatId, "События\n\nevents", kb);
    }

    @Test
    void showProfile_success_sendsProfileWithKeyboard() {
        when(profileService.checkEduLogin("abc")).thenReturn(new ru.izpz.dto.model.ParticipantV1DTO());
        FriendDto friend = FriendDto.builder().login("abc").isFriend(false).build();
        when(profileService.applyFriend(chatId, "abc", FriendRequest.Action.NONE, null)).thenReturn(friend);
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramKeyboardFactory.getFriendInlineKeyboard("abc", friend)).thenReturn(kb);
        ParticipantDto participant = mock(ParticipantDto.class);
        when(profileService.showParticipant(chatId.toString(), "abc")).thenReturn(participant);

        callbackHandler.showProfile(chatId, "abc");

        verify(messageSender).sendMessage(eq(chatId), startsWith("Профиль\n"), eq(kb));
    }

    @Test
    void handleCallbackMessage_setName_setsLastCommandAndAsksForName() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("set_name", Map.of("login", "abc")));

        callbackHandler.handleCallbackMessage(chatId, "data", 1, "cb");

        verify(profileService).setLastCommand(eq(chatId), any(LastCommandState.class));
        verify(messageSender).sendMessage(chatId, "Указать имя", null);
    }

    @Test
    void handleCallbackMessage_event_sendsEventDto() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("event", Map.of("id", "1")));

        EventDto event = new EventDto(1L, "t", "n", null, null, null, null, java.util.List.of(), null, null);
        when(profileService.getEvent(1L)).thenReturn(event);

        callbackHandler.handleCallbackMessage(chatId, "data", 1, "cb");

        verify(messageSender).sendMessage(chatId, event.toString(), null);
    }

    @Test
    void handleCallbackMessage_unknown_sendsUnknownCommandMessage() {
        when(callbackPayloadSerializer.deserialize("data"))
                .thenReturn(new CallbackPayload("unknown", null));

        callbackHandler.handleCallbackMessage(chatId, "data", 1, "cb");

        verify(messageSender).sendMessage(chatId, "Неизвестная команда: data", null);
    }

    @Test
    void showProfile_invalidLogin_sendsValidationMessage() {
        callbackHandler.showProfile(chatId, "12");

        verify(messageSender).sendMessage(chatId, "Логин должен быть от 3 до 30 символов и состоять только из латинских букв", null);
        verifyNoInteractions(profileService);
    }

    @Test
    void showProfile_nullLogin_sendsValidationMessage() {
        callbackHandler.showProfile(chatId, null);

        verify(messageSender).sendMessage(chatId, "Логин должен быть от 3 до 30 символов и состоять только из латинских букв", null);
        verifyNoInteractions(profileService);
    }

    @Test
    void showProfile_whenFeignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        when(profileService.checkEduLogin("abc")).thenThrow(ex);

        callbackHandler.showProfile(chatId, "abc");

        verify(messageSender).sendMessage(chatId, "Ошибка поиска профиля, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void showProfile_whenEduLoginCheckException_sendsUserAndAdmin() {
        ErrorResponseDTO err = new ErrorResponseDTO().status(400).message("bad");
        when(profileService.checkEduLogin("abc")).thenThrow(new EduLoginCheckException(err));

        callbackHandler.showProfile(chatId, "abc");

        verify(messageSender).sendMessage(chatId, "Ошибка проверки логина: bad", null);
        verify(messageSender).sendMessage(999L, "Ошибка проверки логина: " + err, null);
    }

    @Test
    void setLastCommand_whenFeignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        doThrow(ex).when(profileService).setLastCommand(eq(chatId), ArgumentMatchers.any());

        callbackHandler.setLastCommand(chatId, LastCommandType.SEARCH, null);

        verify(messageSender).sendMessage(chatId, "Ошибка установки lastCommand", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void updateMessageAndChangeStatusRegistration_whenFeignException_doesNotThrow() {
        doThrow(createFeignException(500, "boom")).when(profileService).updateProfileStatus(chatId, ProfileStatus.REGISTRATION);

        callbackHandler.updateMessageAndChangeStatusRegistration(chatId, 1, "t");

        verify(messageSender).updateMessage(chatId, 1, "t", null);
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
