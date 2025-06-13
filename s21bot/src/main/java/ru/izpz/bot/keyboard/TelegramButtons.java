package ru.izpz.bot.keyboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelegramButtons {

    public static final String START = "🚀 Начать";
    public static final String PROFILE = "👤 Профиль";
    public static final String SETTINGS = "⚙️ Настройки";
    public static final String HELP = "❓ Помощь";
    public static final String EXIT = "❌ Выйти";
    public static final String SUPPORT = "📞 Поддержка";

    public static final List<String> MAIN_MENU = List.of(START, PROFILE, SETTINGS, HELP, EXIT, SUPPORT);

    // Готовая мапа для inline-клавиатуры
    public static final Map<String, String> INLINE_MENU = Map.of(
            START, "start",
            PROFILE, "profile",
            SETTINGS, "settings",
            HELP, "help"
    );

    public static final Map<String, String> INLINE_MENU_ORDERED = createOrderedMap();

    private static Map<String, String> createOrderedMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(START, "start");
        map.put(PROFILE, "profile");
        map.put(SETTINGS, "settings");
        map.put(HELP, "help");
        map.put(EXIT, "exit");
        return map;
    }

}
