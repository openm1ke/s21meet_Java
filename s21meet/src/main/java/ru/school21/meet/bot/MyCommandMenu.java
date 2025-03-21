package ru.school21.meet.bot;

import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;

import java.util.List;

public class MyCommandMenu {

    public static DeleteMyCommands deleteMenuGroup() {
        DeleteMyCommands deleteMyCommands = new DeleteMyCommands();
        deleteMyCommands.setScope(new BotCommandScopeAllGroupChats());
        return deleteMyCommands;
    }

    public static DeleteMyCommands deleteMenuPrivate() {
        DeleteMyCommands deleteMyCommands = new DeleteMyCommands();
        deleteMyCommands.setScope(new BotCommandScopeAllPrivateChats());
        return deleteMyCommands;
    }

    public static SetMyCommands standartMenuUser() {
        List<BotCommand> commands = List.of(
                new BotCommand("/start", "Start the bot"),
                new BotCommand("/help", "Get help")
        );
        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(commands);
        setMyCommands.setScope(new BotCommandScopeAllPrivateChats());
        return setMyCommands;
    }
}
