package ru.izpz.bot.keyboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.dto.*;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListKeyboardFactoryTest {

    @Mock
    private CallbackPayloadSerializer serializer;

    @InjectMocks
    private ListKeyboardFactory factory;

    @Test
    void createListText_whenEmpty_addsEmptyMarker() {
        String text = factory.createListText(List.of(), "T", x -> "x");
        assertTrue(text.contains("Список пуст"));
    }

    @Test
    void eventsListText_formatsTitleAndItems() {
        EventsSliceDto slice = new EventsSliceDto(
                List.of(new EventDto(1L, "t", "n1", null, null, OffsetDateTime.now(), null, List.of(), 1, 0)),
                0,
                2,
                false
        );

        String text = factory.eventsListText(slice);

        assertTrue(text.contains("События"));
        assertTrue(text.contains("n1"));
    }

    @Test
    void friendsListText_formatsFriendWithBadgesAndOnline() {
        FriendDto f = FriendDto.builder()
                .login("abc")
                .name("Name")
                .isFavorite(true)
                .isSubscribe(true)
                .status(ParticipantStatusEnum.ACTIVE)
                .isOnline(true)
                .clusterName("c")
                .row("r")
                .number(1)
                .build();

        FriendsSliceDto slice = new FriendsSliceDto(List.of(f), 0, 2, false);

        String text = factory.friendsListText(slice);

        assertTrue(text.contains("abc"));
        assertTrue(text.contains("Name"));
        assertTrue(text.contains("⭐"));
        assertTrue(text.contains("\uD83D\uDD14"));
        assertTrue(text.contains(ParticipantStatusEnum.ACTIVE.getEmoji()));
        assertTrue(text.contains("c-r1"));
    }

    @Test
    void friendsListText_formatsOfflineFriendWithoutOptionalFields() {
        FriendDto f = FriendDto.builder()
                .login("abc")
                .name(null)
                .isFavorite(false)
                .isSubscribe(false)
                .status(null)
                .isOnline(false)
                .build();

        FriendsSliceDto slice = new FriendsSliceDto(List.of(f), 0, 2, false);

        String text = factory.friendsListText(slice);

        assertTrue(text.contains("abc"));
        assertTrue(text.contains("❓"));
        assertTrue(text.contains("\uD83D\uDCA4"));
    }

    @Test
    void friendsListText_ignoresBlankName() {
        FriendDto f = FriendDto.builder()
                .login("abc")
                .name("   ")
                .isFavorite(false)
                .isSubscribe(false)
                .status(ParticipantStatusEnum.ACTIVE)
                .isOnline(false)
                .build();

        FriendsSliceDto slice = new FriendsSliceDto(List.of(f), 0, 2, false);

        String text = factory.friendsListText(slice);

        assertTrue(text.contains("abc"));
        assertFalse(text.contains("(   )"));
        assertTrue(text.contains(ParticipantStatusEnum.ACTIVE.getEmoji()));
    }

    @Test
    void friendsListKeyboard_addsPrevAndNextButtonsAccordingToPageAndHasNext() {
        FriendDto f = FriendDto.builder().login("abc").build();
        FriendsSliceDto slice = new FriendsSliceDto(List.of(f), 1, 10, true);

        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.friendsListKeyboard(slice, 2, 1);

        assertNotNull(kb);
        assertEquals(2, kb.getKeyboard().size());
        assertEquals("◀ Назад", kb.getKeyboard().get(0).get(0).getText());
        assertEquals("Вперёд ▶", kb.getKeyboard().get(0).get(1).getText());
        assertEquals("1", kb.getKeyboard().get(1).getFirst().getText());

        verify(serializer, atLeastOnce()).serialize(any(CallbackPayload.class));

        var captor = org.mockito.ArgumentCaptor.forClass(CallbackPayload.class);
        verify(serializer, atLeast(1)).serialize(captor.capture());

        boolean hasPrev = captor.getAllValues().stream()
                .anyMatch(p -> "friends_page".equals(p.getCommand()) && "0".equals(p.getArgs().get("page")));
        boolean hasNext = captor.getAllValues().stream()
                .anyMatch(p -> "friends_page".equals(p.getCommand()) && "2".equals(p.getArgs().get("page")));

        assertTrue(hasPrev);
        assertTrue(hasNext);
    }

    @Test
    void friendsListKeyboard_withoutNavigation_showsOnlyItemButtons() {
        FriendDto f1 = FriendDto.builder().login("a").build();
        FriendDto f2 = FriendDto.builder().login("b").build();
        FriendsSliceDto slice = new FriendsSliceDto(List.of(f1, f2), 0, 10, false);

        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.friendsListKeyboard(slice, 5, 0);

        assertNotNull(kb);
        assertEquals(1, kb.getKeyboard().size());
        assertEquals(2, kb.getKeyboard().getFirst().size());
        assertEquals("1", kb.getKeyboard().getFirst().getFirst().getText());
        assertEquals("2", kb.getKeyboard().getFirst().get(1).getText());
    }

    @Test
    void friendsListKeyboard_firstPage_hasOnlyNextNavigationRow() {
        FriendDto f = FriendDto.builder().login("a").build();
        FriendsSliceDto slice = new FriendsSliceDto(List.of(f), 0, 10, true);
        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.friendsListKeyboard(slice, 5, 0);

        assertNotNull(kb);
        assertEquals(2, kb.getKeyboard().size());
        assertEquals(1, kb.getKeyboard().getFirst().size());
        assertEquals("Вперёд ▶", kb.getKeyboard().getFirst().getFirst().getText());
    }

    @Test
    void friendsListKeyboard_lastPage_hasOnlyPrevNavigationRow() {
        FriendDto f = FriendDto.builder().login("a").build();
        FriendsSliceDto slice = new FriendsSliceDto(List.of(f), 3, 10, false);
        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.friendsListKeyboard(slice, 5, 3);

        assertNotNull(kb);
        assertEquals(2, kb.getKeyboard().size());
        assertEquals(1, kb.getKeyboard().getFirst().size());
        assertEquals("◀ Назад", kb.getKeyboard().getFirst().getFirst().getText());
    }

    @Test
    void eventsListKeyboard_buildsButtonsAndNavigation() {
        EventDto e1 = new EventDto(11L, "talk", "Event 1", null, null, OffsetDateTime.now(), null, List.of(), 10, 2);
        EventDto e2 = new EventDto(12L, "talk", "Event 2", null, null, OffsetDateTime.now(), null, List.of(), 10, 3);
        EventsSliceDto slice = new EventsSliceDto(List.of(e1, e2), 1, 10, true);

        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.eventsListKeyboard(slice, 5, 1);

        assertNotNull(kb);
        assertEquals(2, kb.getKeyboard().size());
        assertEquals("◀ Назад", kb.getKeyboard().get(0).get(0).getText());
        assertEquals("Вперёд ▶", kb.getKeyboard().get(0).get(1).getText());
        assertEquals("1", kb.getKeyboard().get(1).get(0).getText());
        assertEquals("2", kb.getKeyboard().get(1).get(1).getText());
    }

    @Test
    void eventsListKeyboard_withoutNavigation_hasOnlyItemButtons() {
        EventDto e1 = new EventDto(11L, "talk", "Event 1", null, null, OffsetDateTime.now(), null, List.of(), 10, 2);
        EventsSliceDto slice = new EventsSliceDto(List.of(e1), 0, 10, false);

        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.eventsListKeyboard(slice, 5, 0);

        assertNotNull(kb);
        assertEquals(1, kb.getKeyboard().size());
        assertEquals("1", kb.getKeyboard().getFirst().getFirst().getText());
    }
}
