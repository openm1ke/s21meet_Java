package ru.izpz.bot.bot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
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
    Chat chat = mock(Chat.class);
    when(chat.getType()).thenReturn("private");
    when(chat.getUserName()).thenReturn("tester");
    Message message = new Message();
    message.setChat(chat);
    message.setText("Привет, бот!");
    Update update = new Update();
    update.setMessage(message);

    simpleBot.consume(update);

    verify(messageProcessor).handleTextMessage(message);
  }

  @Test
  void consumeShouldNotSendMessageWhenUpdateHasNoTextMessage() {
    Update update = new Update();
    update.setMessage(new Message());

    simpleBot.consume(update);

    verifyNoInteractions(messageProcessor);
  }

  @Test
  void consumeWhenChatNotPrivateDoesNothing() {
    Chat chat = mock(Chat.class);
    when(chat.getType()).thenReturn("group");
    Update update = new Update();
    Message message = new Message();
    message.setChat(chat);
    message.setText("hello");
    update.setMessage(message);

    simpleBot.consume(update);

    verifyNoInteractions(messageProcessor);
  }

  @Test
  void consumeWhenHasCallbackQueryDelegatesToMessageProcessor() {
    Message message = new Message();
    Chat chat = mock(Chat.class);
    when(chat.getId()).thenReturn(10L);
    message.setChat(chat);
    message.setMessageId(5);
    CallbackQuery callbackQuery = new CallbackQuery();
    callbackQuery.setData("data");
    callbackQuery.setId("cb");
    callbackQuery.setMessage(message);
    Update update = new Update();
    update.setCallbackQuery(callbackQuery);

    simpleBot.consume(update);

    verify(messageProcessor).handleCallbackMessage(10L, "data", 5, "cb");
  }
}
