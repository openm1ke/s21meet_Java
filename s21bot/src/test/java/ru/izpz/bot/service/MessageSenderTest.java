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
import ru.izpz.dto.StatusChange;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    @Mock
    private TelegramExecutorService telegramExecutorService;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private MessageSender messageSender;

    @Test
    void execute_success_returnsOptionalWithValue() {
        Message msg = mock(Message.class);
        when(telegramExecutorService.execute(any())).thenReturn(Optional.of(msg));

        Optional<Message> result = messageSender.execute(SendMessage.builder().chatId("1").text("t").build());

        assertTrue(result.isPresent());
        assertEquals(msg, result.get());
    }

    @Test
    void execute_telegramException_returnsEmptyOptional() {
        when(telegramExecutorService.execute(any())).thenReturn(Optional.empty());

        Optional<Message> result = messageSender.execute(SendMessage.builder().chatId("1").text("t").build());

        assertTrue(result.isEmpty());
    }

    @Test
    void sendMessage_buildsSendMessageAndDelegatesToExecute() {
        Message msg = mock(Message.class);
        when(telegramExecutorService.execute(any())).thenReturn(Optional.of(msg));

        ReplyKeyboard kb = mock(ReplyKeyboard.class);
        messageSender.sendMessage(1L, "txt", kb);

        verify(telegramExecutorService).execute(any(SendMessage.class));
    }

    @Test
    void sendStatusChanges_sendsOnlyForValidTelegramIds() {
        Message msg = mock(Message.class);
        when(telegramExecutorService.execute(any())).thenReturn(Optional.of(msg));

        StatusChange c = new StatusChange("login", true, List.of("10", "bad"));

        messageSender.sendStatusChanges(List.of(c));

        verify(telegramExecutorService, times(1)).execute(any(SendMessage.class));
        verify(metricsService).recordNotifyDelivery("success");
    }

    @Test
    void sendStatusChanges_offlineStatus_buildsMessage() {
        when(telegramExecutorService.execute(any())).thenReturn(Optional.empty());

        StatusChange c = new StatusChange("login", false, List.of("10"));

        messageSender.sendStatusChanges(List.of(c));

        verify(telegramExecutorService, times(1)).execute(any(SendMessage.class));
        verify(metricsService).recordNotifyDelivery("error");
    }

    @Test
    void updateMessage_buildsEditMessageTextAndDelegates() {
        Message msg = mock(Message.class);
        when(telegramExecutorService.execute(any())).thenReturn(Optional.of(msg));

        messageSender.updateMessage(1L, 2, "t", mock(org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class));

        verify(telegramExecutorService).execute(any(EditMessageText.class));
    }

    @Test
    void removeInlineKeyboard_buildsEditMessageReplyMarkupAndDelegates() {
        when(telegramExecutorService.execute(any())).thenReturn(Optional.empty());

        messageSender.removeInlineKeyboard(1L, 2);

        verify(telegramExecutorService).execute(any(EditMessageReplyMarkup.class));
    }

    @Test
    void removeReplyKeyboard_buildsSendMessageAndDelegates() {
        when(telegramExecutorService.execute(any())).thenReturn(Optional.empty());

        messageSender.removeReplyKeyboard(1L, "m");

        verify(telegramExecutorService).execute(any(SendMessage.class));
    }

    @Test
    void answerCallbackQuery_buildsMethodAndDelegates() {
        when(telegramExecutorService.execute(any())).thenReturn(Optional.empty());

        messageSender.answerCallbackQuery("cb", "txt", true);

        verify(telegramExecutorService).execute(any(AnswerCallbackQuery.class));
    }
}
