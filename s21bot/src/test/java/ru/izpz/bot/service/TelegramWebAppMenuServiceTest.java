package ru.izpz.bot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import ru.izpz.bot.property.BotProperties;

@ExtendWith(MockitoExtension.class)
class TelegramWebAppMenuServiceTest {

  @Mock private MessageSender messageSender;

  @Mock private BotProperties botProperties;

  private TelegramWebAppMenuService service;

  @BeforeEach
  void setUp() {
    service = new TelegramWebAppMenuService(messageSender, botProperties);
  }

  @Test
  void ensureMenuButton_appliesMenuForValidHttpsUrl() {
    when(botProperties.webAppUrl()).thenReturn("https://webapp.example.org/");
    when(messageSender.execute(any(SetChatMenuButton.class))).thenReturn(Optional.of(true));

    service.ensureMenuButton(1L);

    verify(messageSender).execute(any(SetChatMenuButton.class));
  }

  @Test
  void ensureMenuButton_skipsForPlaceholderUrl() {
    when(botProperties.webAppUrl()).thenReturn("https://REPLACE_WITH_PUBLIC_WEBAPP_DOMAIN/");

    service.ensureMenuButton(1L);

    verify(messageSender, never()).execute(any(SetChatMenuButton.class));
  }

  @Test
  void ensureMenuButton_doesNotReapplyForSameChatAfterSuccess() {
    when(botProperties.webAppUrl()).thenReturn("https://webapp.example.org/");
    when(messageSender.execute(any(SetChatMenuButton.class))).thenReturn(Optional.of(true));

    service.ensureMenuButton(1L);
    service.ensureMenuButton(1L);

    verify(messageSender, times(1)).execute(any(SetChatMenuButton.class));
  }

  @Test
  void ensureMenuButton_skipsWhenChatIdIsNull() {
    service.ensureMenuButton(null);
    verify(messageSender, never()).execute(any(SetChatMenuButton.class));
  }

  @Test
  void ensureMenuButton_skipsForLocalhostUrl() {
    when(botProperties.webAppUrl()).thenReturn("https://localhost:8085/");
    service.ensureMenuButton(2L);
    verify(messageSender, never()).execute(any(SetChatMenuButton.class));
  }

  @Test
  void ensureMenuButton_retriesWhenTelegramDidNotApplyButton() {
    when(botProperties.webAppUrl()).thenReturn("https://webapp.example.org/");
    when(messageSender.execute(any(SetChatMenuButton.class))).thenReturn(Optional.of(false));

    service.ensureMenuButton(3L);
    service.ensureMenuButton(3L);

    verify(messageSender, times(2)).execute(any(SetChatMenuButton.class));
  }
}
