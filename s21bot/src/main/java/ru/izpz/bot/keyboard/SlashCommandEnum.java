package ru.izpz.bot.keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

@RequiredArgsConstructor
@Getter
public enum SlashCommandEnum {
  START("/start", "Показать меню"),
  LOGIN("/login", "Поиск профиля по логину"),
  HELP("/help", "Помощь по командам"),
  ME("/me", "Показать мой Telegram ID"),
  DONATE("/donate", "Поддержать проект");

  private final String command;
  private final String description;

  private static final Map<String, SlashCommandEnum> COMMANDS_MAP =
      Arrays.stream(values()).collect(Collectors.toMap(SlashCommandEnum::getCommand, e -> e));

  public static boolean contains(String text) {
    return parse(text).filter(parsed -> parsed.args().isEmpty()).isPresent();
  }

  public static Optional<SlashCommandEnum> fromText(String text) {
    return parse(text).filter(parsed -> parsed.args().isEmpty()).map(ParsedCommand::command);
  }

  public static Optional<ParsedCommand> parse(String text) {
    if (text == null || text.isBlank() || !text.trim().startsWith("/")) {
      return Optional.empty();
    }

    String[] parts = text.trim().split("\\s+");
    String commandToken = parts[0].toLowerCase(Locale.ROOT);
    int usernameSeparator = commandToken.indexOf('@');
    if (usernameSeparator > 0) {
      commandToken = commandToken.substring(0, usernameSeparator);
    }

    SlashCommandEnum command = COMMANDS_MAP.get(commandToken);
    if (command == null) {
      return Optional.empty();
    }
    return Optional.of(new ParsedCommand(command, Arrays.asList(parts).subList(1, parts.length)));
  }

  public static List<BotCommand> toBotCommands() {
    return Arrays.stream(values())
        .map(command -> new BotCommand(command.command.substring(1), command.description))
        .toList();
  }

  public record ParsedCommand(SlashCommandEnum command, List<String> args) {}
}
