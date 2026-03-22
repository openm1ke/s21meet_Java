package ru.izpz.bot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.izpz.bot.keyboard.MenuCommandEnum;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private ConfirmedFlow confirmedFlow;

    private final Long chatId = 10L;

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

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, "/start");

        verify(messageSender).sendMessage(chatId, "Подпишитесь на канал", urlKb);
        verifyNoMoreInteractions(callbackHandler);
    }

    @Test
    void startConfirmed_whenKickedFromGroup_sendsSubscribeMessageAndReturns() {
        InlineKeyboardMarkup urlKb = mock(InlineKeyboardMarkup.class);
        when(telegramButtons.getSubscribeButton()).thenReturn(Map.of("sub", "url"));
        when(telegramKeyboardFactory.createUrlKeyboard(anyMap(), eq(1))).thenReturn(urlKb);
        ChatMember kickedMember = member("kicked");
        when(messageSender.execute(any())).thenReturn(Optional.of(kickedMember));

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, "/start");

        verify(messageSender).sendMessage(chatId, "Подпишитесь на канал", urlKb);
        verifyNoInteractions(callbackHandler);
    }

    @Test
    void startConfirmed_whenCannotFetchChatMember_sendsSubscribeMessageAndReturns() {
        InlineKeyboardMarkup urlKb = mock(InlineKeyboardMarkup.class);
        when(telegramButtons.getSubscribeButton()).thenReturn(Map.of("sub", "url"));
        when(telegramKeyboardFactory.createUrlKeyboard(anyMap(), eq(1))).thenReturn(urlKb);
        when(messageSender.execute(any())).thenReturn(Optional.empty());

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, "/start");

        verify(messageSender).sendMessage(chatId, "Подпишитесь на канал", urlKb);
        verifyNoInteractions(callbackHandler);
    }

    @Test
    void startConfirmed_slashStart_sendsMenuKeyboard() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ReplyKeyboardMarkup menuKb = mock(ReplyKeyboardMarkup.class);
        when(telegramKeyboardFactory.createReplyKeyboard(anyList(), eq(3))).thenReturn(menuKb);

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, "/start");

        verify(messageSender).sendMessage(chatId, "Выберите команду", menuKb);
    }

    @Test
    void startConfirmed_slashMe_sendsTelegramId() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, "/me");

        verify(messageSender).sendMessage(chatId, "Твой telegram id: " + profile.telegramId(), null);
    }

    @Test
    void startConfirmed_slashHelp_sendsHelp() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, "/help");

        verify(messageSender).sendMessage(chatId, "Помощь по командам бота", null);
    }

    @Test
    void startConfirmed_slashDonate_sendsDonateText() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, "/donate");

        verify(messageSender).sendMessage(eq(chatId), contains("На работу бота"), eq(null));
    }

    @Test
    void startConfirmed_menuFriends_callsCallbackHandlerShowFriends() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, "😸 Друзья");

        verify(callbackHandler).showFriends(chatId, 0, null);
    }

    @Test
    void startConfirmed_menuSearch_setsLastCommandAndPromptsLogin() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.SEARCH.getCommand());

        verify(callbackHandler).setLastCommand(chatId, LastCommandType.SEARCH, null);
        verify(messageSender).sendMessage(chatId, "Введите логин для поиска", null);
    }

    @Test
    void startConfirmed_menuProfile_callsProfileServiceAndSendsProfile() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("abc");
        participant.setClassName("22_10_MSK");
        participant.setExpValue(21617);
        participant.setLevel(12);
        participant.setParallelName("AP4");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        when(profileService.showParticipant(chatId.toString(), "abc")).thenReturn(participant);
        when(profileService.getProjects("abc")).thenReturn(List.of(
                new ProjectsDto("g1", "Libft", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                new ProjectsDto("g2", "minishell", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        ));

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.PROFILE.getCommand());

        verify(profileService).showParticipant(chatId.toString(), "abc");
        verify(profileService).getProjects("abc");
        verify(messageSender).sendMessage(eq(chatId), argThat(message ->
                message.contains("✅ abc")
                        && message.contains("🌊22_10_MSK")
                        && message.contains("✨21617 XP (level 12)")
                        && message.contains("👨‍💻AP4")
                        && message.contains("📁Libft, minishell")
        ), eq(null));
    }

    @Test
    void startConfirmed_menuEvents_callsCallbackHandlerShowEvents() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.EVENTS.getCommand());

        verify(callbackHandler).showEvents(chatId, 0, null);
    }

    @Test
    void startConfirmed_menuCampus_callsProfileServiceAndSendsCampus() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        CampusResponse campus = new CampusResponse();
        campus.setCampusName("Moscow");
        campus.setClusters(List.of(
                new Clusters("Illusion", 59, 20, 2),
                new Clusters("Mirage", 138, 125, 2),
                new Clusters("Supernova", 54, 53, 3),
                new Clusters("Pegasus", 10, 5, 17)
        ));
        campus.setProgramStats(Map.of(
                "Data Science", 39L,
                "👽 Intensive Parallel 76", 13L,
                "No data", 1L
        ));
        when(profileService.showCampusMap(chatId)).thenReturn(campus);

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.CAMPUS.getCommand());

        verify(profileService).showCampusMap(chatId);
        verify(messageSender).sendMessage(eq(chatId), argThat(message ->
                message.contains("🏕️ Moscow campus 🎪")
                        && message.contains("🪑 Busy 58 / Free 203 / All 261")
                        && message.contains("2️⃣ Floor")
                        && message.contains("🔸 Illusion - 39 / 20 / 59")
                        && message.contains("3️⃣ Floor")
                        && message.contains("🔹 Supernova - 1 / 53 / 54")
                        && message.contains("1️⃣7️⃣ Floor")
                        && message.contains("🔹 Pegasus - 5 / 5 / 10")
                        && message.contains("🧢 Data Science: 39")
                        && message.contains("⚡ 👽 Intensive Parallel 76: 13")
                        && message.contains("👽 No data: 1")
                        && message.indexOf("🧢 Data Science: 39") < message.indexOf("⚡ 👽 Intensive Parallel 76: 13")
                        && message.indexOf("⚡ 👽 Intensive Parallel 76: 13") < message.indexOf("👽 No data: 1")
        ), eq(null));
    }

    @Test
    void startConfirmed_menuCampus_whenClustersAndProgramsMissing_showsHeaderOnly() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        CampusResponse campus = new CampusResponse();
        campus.setCampusName("Kazan");
        campus.setClusters(null);
        campus.setProgramStats(null);
        when(profileService.showCampusMap(chatId)).thenReturn(campus);

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.CAMPUS.getCommand());

        verify(messageSender).sendMessage(eq(chatId), argThat(message ->
                message.contains("🏕️ Kazan campus 🎪")
                        && message.contains("🪑 Busy 0 / Free 0 / All 0")
                        && !message.contains("Floor")
        ), eq(null));
    }

    @Test
    void startConfirmed_menuCampus_formatsAllFloorDigitsAndNullClusterValues() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        CampusResponse campus = new CampusResponse();
        campus.setCampusName("Kazan");
        campus.setClusters(List.of(
                new Clusters(null, null, -1, 1234567890)
        ));
        campus.setProgramStats(Map.of("Parallel 99", 2L));
        when(profileService.showCampusMap(chatId)).thenReturn(campus);

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.CAMPUS.getCommand());

        verify(messageSender).sendMessage(eq(chatId), argThat(message ->
                message.contains("1️⃣2️⃣3️⃣4️⃣5️⃣6️⃣7️⃣8️⃣9️⃣0️⃣ Floor")
                        && message.contains("🔸  - 0 / 0 / 0")
                        && message.contains("⚡ Parallel 99: 2")
        ), eq(null));
    }

    @Test
    void startConfirmed_menuProjects_whenEmpty_sendsNoActiveProjects() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        when(profileService.getProjects("abc")).thenReturn(java.util.Collections.emptyList());

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.PROJECTS.getCommand());

        verify(messageSender).sendMessage(chatId, "нет активных проектов", null);
    }

    @Test
    void startConfirmed_menuProjects_whenNotEmpty_formatsProjectsList() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        ProjectsDto p1 = new ProjectsDto(
                "g1",
                "AP4_Info21 v2.0 Web_Jv",
                "Desc",
                10,
                null, // dateTime
                null, // finalPercentage
                null, // laboriousness
                "INDIVIDUAL", // executionType
                "IN_PROGRESS", // goalStatus
                "PROJECT", // courseType
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
                "DevOps Exam",
                "",
                200,
                null, // dateTime
                null, // finalPercentage
                null, // laboriousness
                "EXAM_TEST", // executionType
                null, // goalStatus
                "EXAM", // courseType
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
                "BE4_GRPCAuth",
                null,
                600,
                null,
                null,
                null,
                "GROUP",
                null,
                "PROJECT",
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

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.PROJECTS.getCommand());

        verify(messageSender).sendMessage(eq(chatId), argThat(t ->
                t.contains("🖥️ Мои активные проекты 💼")
                        && t.contains("1. 👨🏻‍💻 AP4_Info21 v2.0 Web_Jv (10xp)")
                        && t.contains("2. ✍️ DevOps Exam (200xp)")
                        && t.contains("3. 👥 BE4_GRPCAuth (600xp)")), eq(null));
    }

    @Test
    void startConfirmed_lastCommandSetName_whenTooLong_sendsValidationErrorAndClearsLastCommand() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        LastCommandState state = new LastCommandState(LastCommandType.SET_NAME, Map.of("login", "abc"));
        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, state);

        confirmedFlow.startConfirmed(chatId, profile, "x".repeat(101));

        verify(messageSender).sendMessage(chatId, "Имя должно быть не более 100 символов", null);
        verify(callbackHandler).setLastCommand(chatId, null, null);
    }

    @Test
    void startConfirmed_lastCommandSearch_callsCallbackHandlerShowProfileAndClearsLastCommand() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        LastCommandState state = new LastCommandState(LastCommandType.SEARCH, null);
        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, state);

        confirmedFlow.startConfirmed(chatId, profile, "xyz");

        verify(callbackHandler).showProfile(chatId, "xyz");
        verify(callbackHandler).setLastCommand(chatId, null, null);
    }

    @Test
    void startConfirmed_lastCommandSetName_updatesFriendAndClearsLastCommand() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        LastCommandState state = new LastCommandState(LastCommandType.SET_NAME, Map.of("login", "abc"));
        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, state);

        confirmedFlow.startConfirmed(chatId, profile, "name");

        verify(profileService).applyFriend(chatId, "abc", FriendRequest.Action.SET_NAME, "name");
        verify(messageSender).sendMessage(chatId, "Имя успешно обновлено", null);
        verify(callbackHandler).setLastCommand(chatId, null, null);
    }

    @Test
    void startConfirmed_lastCommandNone_recordsMetricAndOnlyClearsLastCommand() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        LastCommandState state = new LastCommandState(LastCommandType.NONE, Map.of("login", "abc"));
        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, state);

        confirmedFlow.startConfirmed(chatId, profile, "any");

        verify(metricsService).recordButtonPress(LastCommandType.NONE.name(), ButtonMetricType.LAST_COMMAND);
        verify(callbackHandler).setLastCommand(chatId, null, null);
        verify(profileService, never()).applyFriend(anyLong(), anyString(), any(), anyString());
        verify(callbackHandler, never()).showProfile(anyLong(), anyString());
    }

    @Test
    void startConfirmed_menuCampus_handlesNegativeFloorBlankCampusAndNullProgramValues() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        CampusResponse campus = new CampusResponse();
        campus.setCampusName(" ");
        campus.setClusters(List.of(
                new Clusters("  ", 2, 5, -1)
        ));
        java.util.LinkedHashMap<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put(null, null);
        campus.setProgramStats(stats);
        when(profileService.showCampusMap(chatId)).thenReturn(campus);

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.CAMPUS.getCommand());

        verify(messageSender).sendMessage(eq(chatId), argThat(message ->
                message.contains("🏕️  campus 🎪")
                        && message.contains("🪑 Busy -3 / Free 5 / All 2")
                        && message.contains("-1️⃣ Floor")
                        && message.contains("🔹  - 0 / 5 / 2")
                        && message.contains("🧢 : 0")
        ), eq(null));
    }

    @Test
    void startConfirmed_menuCampus_handlesNullFloorAndEmptyProgramStats() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        CampusResponse campus = new CampusResponse();
        campus.setCampusName("Kazan");
        campus.setClusters(List.of(new Clusters("A", 1, 1, null)));
        campus.setProgramStats(Map.of());
        when(profileService.showCampusMap(chatId)).thenReturn(campus);

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.CAMPUS.getCommand());

        verify(messageSender).sendMessage(eq(chatId), argThat(message ->
                message.contains("0️⃣ Floor")
                        && message.contains("🔸 A - 0 / 1 / 1")
                        && !message.contains(": 0\n")
        ), eq(null));
    }

    @Test
    void startConfirmed_menuCampus_sortsProgramStatsWithNullAndNonNullValues() {
        ChatMember member = member("member");
        when(messageSender.execute(any())).thenReturn(Optional.of(member));

        CampusResponse campus = new CampusResponse();
        campus.setCampusName("Kazan");
        campus.setClusters(List.of());
        java.util.LinkedHashMap<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put("Parallel 99", null);
        stats.put("Data Science", 1L);
        campus.setProgramStats(stats);
        when(profileService.showCampusMap(chatId)).thenReturn(campus);

        ProfileDto profile = new ProfileDto(chatId.toString(), "abc", ProfileStatus.CONFIRMED, null);
        confirmedFlow.startConfirmed(chatId, profile, MenuCommandEnum.CAMPUS.getCommand());

        verify(messageSender).sendMessage(eq(chatId), argThat(message ->
                message.contains("⚡ Parallel 99: 0")
                        && message.contains("🧢 Data Science: 1")
        ), eq(null));
    }

    private ChatMember member(String status) {
        ChatMember m = mock(ChatMember.class);
        doReturn(status).when(m).getStatus();
        return m;
    }
}
