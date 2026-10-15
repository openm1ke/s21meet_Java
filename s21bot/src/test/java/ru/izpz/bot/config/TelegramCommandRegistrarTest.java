package ru.izpz.bot.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import ru.izpz.bot.service.MessageSender;

@ExtendWith(MockitoExtension.class)
class TelegramCommandRegistrarTest {

  @Mock private MessageSender messageSender;

  @Test
  void registerCommands_sendsPrivateCommandsToTelegram() {
    when(messageSender.execute(any(SetMyCommands.class))).thenReturn(Optional.of(true));

    new TelegramCommandRegistrar(messageSender).registerCommands();

    ArgumentCaptor<SetMyCommands> captor = ArgumentCaptor.forClass(SetMyCommands.class);
    verify(messageSender).execute(captor.capture());
    assertTrue(
        captor.getValue().getCommands().stream()
            .anyMatch(command -> "login".equals(command.getCommand())));
  }
}
