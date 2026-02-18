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
import ru.izpz.bot.keyboard.MenuCommandEnum;
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
    void startConfirmed_whenKickedFromGroup_sendsSubscribeMessageAndReturns() {
        InlineKeyboardMarkup urlKb = mock(InlineKeyboardMarkup.class);
        when(telegramButtons.getSubscribeButton()).thenReturn(Map.of("sub", "url"));
        when(telegramKeyboardFactory.createUrlKeyboard(anyMap(), eq(1))).thenReturn(urlKb);
        ChatMember kickedMember = member("kicked");
        when(messageSender.execute(any())).thenReturn(Optional.of(kickedMember));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, "/start");

        verify(messageSender).sendMessage(CHAT_ID, "Подпишитесь на канал", urlKb);
        verifyNoInteractions(callbackHandler);
    }

    @Test
    void startConfirmed_whenCannotFetchChatMember_sendsSubscribeMessageAndReturns() {
        InlineKeyboardMarkup urlKb = mock(InlineKeyboardMarkup.class);
        when(telegramButtons.getSubscribeButton()).thenReturn(Map.of("sub", "url"));
        when(telegramKeyboardFactory.createUrlKeyboard(anyMap(), eq(1))).thenReturn(urlKb);
        when(messageSender.execute(any())).thenReturn(Optional.empty());

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, "/start");

        verify(messageSender).sendMessage(CHAT_ID, "Подпишитесь на канал", urlKb);
        verifyNoInteractions(callbackHandler);
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
    void startConfirmed_slashMe_sendsTelegramId() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, "/me");

        verify(messageSender).sendMessage(CHAT_ID, "Твой telegram id: " + profile.telegramId(), null);
    }

    @Test
    void startConfirmed_slashHelp_sendsHelp() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, "/help");

        verify(messageSender).sendMessage(CHAT_ID, "Помощь по командам бота", null);
    }

    @Test
    void startConfirmed_slashDonate_sendsDonateText() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, "/donate");

        verify(messageSender).sendMessage(eq(CHAT_ID), contains("На работу бота"), eq(null));
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
    void startConfirmed_menuSearch_setsLastCommandAndPromptsLogin() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, MenuCommandEnum.SEARCH.getCommand());

        verify(callbackHandler).setLastCommand(CHAT_ID, LastCommandType.SEARCH, null);
        verify(messageSender).sendMessage(CHAT_ID, "Введите логин для поиска", null);
    }

    @Test
    void startConfirmed_menuProfile_callsProfileServiceAndSendsProfile() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ParticipantDto participant = mock(ParticipantDto.class);
        when(profileService.showParticipant(CHAT_ID.toString(), "abc")).thenReturn(participant);

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, MenuCommandEnum.PROFILE.getCommand());

        verify(profileService).showParticipant(CHAT_ID.toString(), "abc");
        verify(messageSender).sendMessage(eq(CHAT_ID), startsWith("Профиль\n"), eq(null));
    }

    @Test
    void startConfirmed_menuEvents_callsCallbackHandlerShowEvents() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, MenuCommandEnum.EVENTS.getCommand());

        verify(callbackHandler).showEvents(CHAT_ID, 0, null);
    }

    @Test
    void startConfirmed_menuCampus_callsProfileServiceAndSendsCampus() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        CampusResponse campus = new CampusResponse();
        campus.setCampusName("X");
        when(profileService.showCampusMap(CHAT_ID)).thenReturn(campus);

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, MenuCommandEnum.CAMPUS.getCommand());

        verify(profileService).showCampusMap(CHAT_ID);
        verify(messageSender).sendMessage(eq(CHAT_ID), contains("Кампус X"), eq(null));
    }

    @Test
    void startConfirmed_menuProjects_whenEmpty_sendsNoActiveProjects() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        when(profileService.getProjects("abc")).thenReturn(java.util.Collections.emptyList());

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, MenuCommandEnum.PROJECTS.getCommand());

        verify(messageSender).sendMessage(CHAT_ID, "У вас нет активных проектов", null);
    }

    @Test
    void startConfirmed_menuProjects_whenNotEmpty_formatsProjectsList() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProjectsDto p1 = new ProjectsDto(
                "g1",
                "Project1",
                "Desc",
                10,
                null, // dateTime
                null, // finalPercentage
                null, // laboriousness
                null, // executionType
                null, // goalStatus
                null, // courseType
                "IN_PROGRESS", // displayedCourseStatus
                null,
                null,
                null,
                null,
                null,
                null,
                "GroupA",
                1
        );
        ProjectsDto p2 = new ProjectsDto(
                "g2",
                "Project2",
                "",
                null,
                null, // dateTime
                null, // finalPercentage
                null, // laboriousness
                null, // executionType
                null, // goalStatus
                null, // courseType
                null, // displayedCourseStatus
                null, // amountAnswers
                null, // amountMembers
                null, // amountJoinedMembers
                null, // amountReviewedAnswers
                null, // amountCodeReviewMembers
                null, // amountCurrentCodeReviewMembers
                "GroupB",
                2
        );

        ProjectsDto p3 = new ProjectsDto(
                "g3",
                "Project3",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "GroupC",
                3
        );
        when(profileService.getProjects("abc")).thenReturn(java.util.List.of(p1, p2, p3));

        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(CHAT_ID, profile, MenuCommandEnum.PROJECTS.getCommand());

        verify(messageSender).sendMessage(eq(CHAT_ID), argThat(t ->
                t.contains("Ваши активные проекты")
                        && t.contains("Project1")
                        && t.contains("Desc")
                        && t.contains("Опыт: 10")
                        && t.contains("Статус: IN_PROGRESS")
                        && t.contains("Project2")
                        && t.contains("Project3")), eq(null));
    }

    @Test
    void startConfirmed_lastCommandSetName_whenTooLong_sendsValidationErrorAndClearsLastCommand() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        LastCommandState state = new LastCommandState(LastCommandType.SET_NAME, Map.of("login", "abc"));
        ProfileDto profile = new ProfileDto(CHAT_ID.toString(), "abc", ProfileStatus.CONFIRMED, state);

        confirmedFlow.startConfirmed(CHAT_ID, profile, "x".repeat(101));

        verify(messageSender).sendMessage(CHAT_ID, "Имя должно быть не более 100 символов", null);
        verify(callbackHandler).setLastCommand(CHAT_ID, null, null);
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
