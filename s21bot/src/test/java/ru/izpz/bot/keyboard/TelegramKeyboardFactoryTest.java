package ru.izpz.bot.keyboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.dto.FriendDto;
import ru.izpz.dto.FriendsSliceDto;
import ru.izpz.dto.EventsSliceDto;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramKeyboardFactoryTest {

    @Mock
    private CallbackPayloadSerializer serializer;

    @Mock
    private ListKeyboardFactory listKeyboardFactory;

    @InjectMocks
    private TelegramKeyboardFactory factory;

    @Test
    void createReplyKeyboard_wrapsRows() {
        ReplyKeyboardMarkup kb = factory.createReplyKeyboard(List.of("a", "b", "c", "d"), 3);
        assertNotNull(kb);
        assertEquals(2, kb.getKeyboard().size());
        assertEquals(3, kb.getKeyboard().get(0).size());
        assertEquals(1, kb.getKeyboard().get(1).size());
    }

    @Test
    void createUrlKeyboard_createsButtonsWithUrls() {
        LinkedHashMap<String, String> buttons = new LinkedHashMap<>();
        buttons.put("t1", "u1");
        buttons.put("t2", "u2");

        InlineKeyboardMarkup kb = factory.createUrlKeyboard(buttons, 1);
        assertNotNull(kb);
        assertEquals(2, kb.getKeyboard().size());
        assertEquals("t1", kb.getKeyboard().getFirst().getFirst().getText());
        assertEquals("u1", kb.getKeyboard().getFirst().getFirst().getUrl());
    }

    @Test
    void createUrlKeyboard_exactRowSize_hasNoTrailingRow() {
        LinkedHashMap<String, String> buttons = new LinkedHashMap<>();
        buttons.put("t1", "u1");
        buttons.put("t2", "u2");

        InlineKeyboardMarkup kb = factory.createUrlKeyboard(buttons, 2);

        assertNotNull(kb);
        assertEquals(1, kb.getKeyboard().size());
        assertEquals(2, kb.getKeyboard().getFirst().size());
    }

    @Test
    void createInlineKeyboardMarkup_createsButtonsWithCallbackData() {
        LinkedHashMap<String, String> buttons = new LinkedHashMap<>();
        buttons.put("t1", "d1");
        buttons.put("t2", "d2");

        InlineKeyboardMarkup kb = factory.createInlineKeyboardMarkup(buttons, 1);
        assertNotNull(kb);
        assertEquals("d1", kb.getKeyboard().get(0).getFirst().getCallbackData());
        assertEquals("d2", kb.getKeyboard().get(1).getFirst().getCallbackData());
    }

    @Test
    void eventsListKeyboard_delegatesToListKeyboardFactory() {
        EventsSliceDto slice = new EventsSliceDto(java.util.Collections.emptyList(), 0, 2, false);
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(listKeyboardFactory.eventsListKeyboard(slice, 3, 0)).thenReturn(kb);

        assertEquals(kb, factory.eventsListKeyboard(slice, 3, 0));
    }

    @Test
    void friendsListKeyboard_delegatesToListKeyboardFactory() {
        FriendsSliceDto slice = new FriendsSliceDto(java.util.Collections.emptyList(), 0, 2, false);
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(listKeyboardFactory.friendsListKeyboard(slice, 3, 0)).thenReturn(kb);

        assertEquals(kb, factory.friendsListKeyboard(slice, 3, 0));
    }

    @Test
    void createAnswerCallbackQuery_setsFields() {
        AnswerCallbackQuery q = factory.createAnswerCallbackQuery("id", "txt", true);
        assertEquals("txt", q.getText());
        assertTrue(q.getShowAlert());
    }

    @Test
    void getFriendInlineKeyboard_whenNotFriend_hasOnlyAddFriendButton() {
        FriendDto f = FriendDto.builder()
                .isFriend(false)
                .isFavorite(false)
                .isSubscribe(false)
                .build();

        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.getFriendInlineKeyboard("login", f);

        assertNotNull(kb);
        List<String> callbackData = kb.getKeyboard().stream()
                .flatMap(List::stream)
                .map(InlineKeyboardButton::getCallbackData)
                .toList();

        assertTrue(callbackData.contains("cb"));
    }

    @Test
    void getFriendInlineKeyboard_whenFriend_addsExtraButtons() {
        FriendDto f = FriendDto.builder()
                .isFriend(true)
                .isFavorite(true)
                .isSubscribe(true)
                .name("n")
                .build();

        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("cb");

        InlineKeyboardMarkup kb = factory.getFriendInlineKeyboard("login", f);

        assertNotNull(kb);
        List<String> callbackData = kb.getKeyboard().stream()
                .flatMap(List::stream)
                .map(InlineKeyboardButton::getCallbackData)
                .toList();

        assertEquals(4, callbackData.size());
        assertTrue(callbackData.stream().allMatch("cb"::equals));
    }
}
