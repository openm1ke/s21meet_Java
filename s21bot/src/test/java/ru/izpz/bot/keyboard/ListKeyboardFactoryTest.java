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
    void friendsListKeyboard_addsPrevAndNextButtonsAccordingToPageAndHasNext() {
        FriendDto f = FriendDto.builder().login("abc").build();
        FriendsSliceDto slice = new FriendsSliceDto(List.of(f), 1, 2, true);

        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.friendsListKeyboard(slice, 2, 1);

        assertNotNull(kb);

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
}
