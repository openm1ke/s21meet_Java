package ru.izpz.bot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.izpz.dto.StatusChange;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    @Mock
    private TelegramClientProxy telegramClientProxy;

    @InjectMocks
    private MessageSender messageSender;

    @Test
    void execute_success_returnsOptionalWithValue() throws TelegramApiException {
        Message msg = mock(Message.class);
        when(telegramClientProxy.execute(any())).thenReturn(msg);

        Optional<Message> result = messageSender.execute(SendMessage.builder().chatId("1").text("t").build());

        assertTrue(result.isPresent());
        assertEquals(msg, result.get());
    }

    @Test
    void execute_telegramException_returnsEmptyOptional() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenThrow(new TelegramApiException("err"));

        Optional<Message> result = messageSender.execute(SendMessage.builder().chatId("1").text("t").build());

        assertTrue(result.isEmpty());
    }

    @Test
    void sendMessage_buildsSendMessageAndDelegatesToExecute() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenReturn(null);

        ReplyKeyboard kb = mock(ReplyKeyboard.class);
        messageSender.sendMessage(1L, "txt", kb);

        verify(telegramClientProxy).execute(any(SendMessage.class));
    }

    @Test
    void sendStatusChanges_sendsOnlyForValidTelegramIds() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenReturn(null);

        StatusChange c = new StatusChange("login", true, List.of("10", "bad"));

        messageSender.sendStatusChanges(List.of(c));

        verify(telegramClientProxy, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void sendStatusChanges_offlineStatus_buildsMessage() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenReturn(null);

        StatusChange c = new StatusChange("login", false, List.of("10"));

        messageSender.sendStatusChanges(List.of(c));

        verify(telegramClientProxy, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void updateMessage_buildsEditMessageTextAndDelegates() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenReturn(null);

        messageSender.updateMessage(1L, 2, "t", mock(org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class));

        verify(telegramClientProxy).execute(any(EditMessageText.class));
    }

    @Test
    void removeInlineKeyboard_buildsEditMessageReplyMarkupAndDelegates() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenReturn(null);

        messageSender.removeInlineKeyboard(1L, 2);

        verify(telegramClientProxy).execute(any(EditMessageReplyMarkup.class));
    }

    @Test
    void removeReplyKeyboard_buildsSendMessageAndDelegates() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenReturn(null);

        messageSender.removeReplyKeyboard(1L, "m");

        verify(telegramClientProxy).execute(any(SendMessage.class));
    }

    @Test
    void answerCallbackQuery_buildsMethodAndDelegates() throws TelegramApiException {
        when(telegramClientProxy.execute(any())).thenReturn(null);

        messageSender.answerCallbackQuery("cb", "txt", true);

        verify(telegramClientProxy).execute(any(AnswerCallbackQuery.class));
    }
}
