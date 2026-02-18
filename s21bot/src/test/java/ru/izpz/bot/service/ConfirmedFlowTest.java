package ru.izpz.bot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.*;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmedFlowTest {

    @Mock
    private BotProperties botProperties;

    @Mock
    private ProfileService profileService;

    @Mock
    private TelegramButtons telegramButtons;

    @Mock
    private MessageSender messageSender;

    @Mock
    private TelegramKeyboardFactory telegramKeyboardFactory;

    @Mock
    private CallbackHandler callbackHandler;

    @InjectMocks
    private ConfirmedFlow confirmedFlow;

    private final Long CHAT_ID = 10L;

    @BeforeEach
    void setUp() {
        when(botProperties.group()).thenReturn(777L);
    }

    @Test
    void startConfirmed_whenNotInGroup_sendsSubscribeMessageAndReturns() {
        InlineKeyboardMarkup urlKb = mock(InlineKeyboardMarkup.class);
        when(telegramButtons.getSubscribeButton()).thenReturn(Map.of("sub", "url"));
        when(telegramKeyboardFactory.createUrlKeyboard(anyMap(), eq(1))).thenReturn(urlKb);
        ChatMember leftMember = member("left");
        when(messageSender.execute(any())).thenReturn(Optional.of(leftMember));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, "/start");

        verify(messageSender).sendMessage(CHAT_ID, "Подпишитесь на канал", urlKb);
        verifyNoMoreInteractions(callbackHandler);
    }

    @Test
    void startConfirmed_slashStart_sendsMenuKeyboard() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ReplyKeyboardMarkup menuKb = mock(ReplyKeyboardMarkup.class);
        when(telegramKeyboardFactory.createReplyKeyboard(anyList(), eq(3))).thenReturn(menuKb);

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, "/start");

        verify(messageSender).sendMessage(CHAT_ID, "Выберите команду", menuKb);
    }

    @Test
    void startConfirmed_menuFriends_callsCallbackHandlerShowFriends() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, "😸 Друзья");

        verify(callbackHandler).showFriends(CHAT_ID, 0, null);
    }

    @Test
    void startConfirmed_lastCommandSearch_callsCallbackHandlerShowProfileAndClearsLastCommand() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        LastCommandState state = new LastCommandState(LastCommandType.SEARCH, null);
        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, state);

        confirmedFlow.startConfirmed(CHAT_ID, profile, "xyz");

        verify(callbackHandler).showProfile(CHAT_ID, "xyz");
        verify(callbackHandler).setLastCommand(CHAT_ID, null, null);
    }

    @Test
    void startConfirmed_lastCommandSetName_updatesFriendAndClearsLastCommand() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        LastCommandState state = new LastCommandState(LastCommandType.SET_NAME, Map.of("login", "abc"));
        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, state);

        confirmedFlow.startConfirmed(CHAT_ID, profile, "name");

        verify(profileService).applyFriend(CHAT_ID, "abc", FriendRequest.Action.SET_NAME, "name");
        verify(messageSender).sendMessage(CHAT_ID, "Имя успешно обновлено", null);
        verify(callbackHandler).setLastCommand(CHAT_ID, null, null);
    }

    private ChatMember member(String status) {
        ChatMember m = mock(ChatMember.class);
        doReturn(status).when(m).getStatus();
        return m;
    }
}
