package ru.izpz.bot.keyboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SlashCommandEnumTest {

  @Test
  void parse_loginCommand_returnsLoginAndArgument() {
    var parsed = SlashCommandEnum.parse("/login quarkron");

    assertTrue(parsed.isPresent());
    assertEquals(SlashCommandEnum.LOGIN, parsed.get().command());
    assertEquals(List.of("quarkron"), parsed.get().args());
  }

  @Test
  void parse_commandWithBotUsername_isSupported() {
    var parsed = SlashCommandEnum.parse("/login@my_bot QuArKrOn");

    assertTrue(parsed.isPresent());
    assertEquals(SlashCommandEnum.LOGIN, parsed.get().command());
    assertEquals(List.of("QuArKrOn"), parsed.get().args());
  }

  @Test
  void parse_startDeepLink_keepsParameter() {
    var parsed = SlashCommandEnum.parse("/start login_quarkron");

    assertTrue(parsed.isPresent());
    assertEquals(SlashCommandEnum.START, parsed.get().command());
    assertEquals(List.of("login_quarkron"), parsed.get().args());
  }

  @Test
  void toBotCommands_containsLoginCommand() {
    assertTrue(
        SlashCommandEnum.toBotCommands().stream()
            .anyMatch(command -> "login".equals(command.getCommand())));
  }
}
