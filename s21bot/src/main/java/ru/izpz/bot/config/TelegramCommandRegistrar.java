package ru.izpz.bot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import ru.izpz.bot.keyboard.SlashCommandEnum;
import ru.izpz.bot.service.MessageSender;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bot.session.enabled", havingValue = "true", matchIfMissing = true)
public class TelegramCommandRegistrar {

  private final MessageSender messageSender;

  @EventListener(ApplicationReadyEvent.class)
  public void registerCommands() {
    SetMyCommands request =
        SetMyCommands.builder()
            .commands(SlashCommandEnum.toBotCommands())
            .scope(new BotCommandScopeAllPrivateChats())
            .build();
    boolean registered = messageSender.execute(request).orElse(false);
    if (!registered) {
      log.warn("Не удалось зарегистрировать slash-команды Telegram-бота");
    }
  }
}
