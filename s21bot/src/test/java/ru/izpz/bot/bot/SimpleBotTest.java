package ru.izpz.bot.bot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.izpz.bot.service.MessageProcessor;

class SimpleBotTest {

  private final MessageProcessor messageProcessor = mock(MessageProcessor.class);
  private final SimpleBot simpleBot = new SimpleBot(messageProcessor);

  @Test
  void consumeShouldSendCorrectResponseWhenUpdateHasTextMessage() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    Chat chat = mock(Chat.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(true);
    when(message.getChatId()).thenReturn(12345L);
    when(message.getChat()).thenReturn(chat);
    when(chat.getType()).thenReturn("private");
    when(message.getText()).thenReturn("Привет, бот!");

    simpleBot.consume(update);

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

    verify(messageProcessor, times(1)).handleTextMessage(captor.capture());
  }

  @Test
  void consumeShouldNotSendMessageWhenUpdateHasNoTextMessage() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(false);

    simpleBot.consume(update);

    verifyNoInteractions(messageProcessor);
  }

  @Test
  void consumeWhenChatNotPrivateDoesNothing() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    Chat chat = mock(Chat.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(true);
    when(message.getChat()).thenReturn(chat);
    when(chat.getType()).thenReturn("group");

    simpleBot.consume(update);

    verifyNoInteractions(messageProcessor);
  }

  @Test
  void consumeWhenHasCallbackQueryDelegatesToMessageProcessor() {
    Update update = mock(Update.class);
    CallbackQuery callbackQuery = mock(CallbackQuery.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(false);
    when(update.hasCallbackQuery()).thenReturn(true);
    when(update.getCallbackQuery()).thenReturn(callbackQuery);
    when(callbackQuery.getData()).thenReturn("data");
    when(callbackQuery.getId()).thenReturn("cb");
    when(callbackQuery.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(10L);
    when(message.getMessageId()).thenReturn(5);

    simpleBot.consume(update);

    verify(messageProcessor).handleCallbackMessage(10L, "data", 5, "cb");
  }
}
