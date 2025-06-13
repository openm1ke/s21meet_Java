package ru.izpz.bot.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TelegramKeyboardFactory {

    public static ReplyKeyboardMarkup createReplyKeyboard(List<String> buttonTexts, int buttonsPerRow) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        for (int i = 0; i < buttonTexts.size(); i++) {
            row.add(new KeyboardButton(buttonTexts.get(i)));

            if ((i + 1) % buttonsPerRow == 0) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }

        if (!row.isEmpty()) {
            keyboard.add(row);
        }

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

    public static InlineKeyboardMarkup createInlineKeyboardMarkup(Map<String, String> buttons, int rowSize) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (Map.Entry<String, String> entry : buttons.entrySet()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(entry.getKey())
                    .callbackData(entry.getValue())
                    .build();
            currentRow.add(button);

            if (currentRow.size() == rowSize) {
                rows.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public static ReplyKeyboard removeReplyKeyboard() {
        return new ReplyKeyboardRemove(true);
    }

    /**
     * Упрощённый метод для обычной клавиатуры 3 в ряд.
     */
    public static ReplyKeyboardMarkup defaultReplyKeyboard(List<String> buttonTexts) {
        return createReplyKeyboard(buttonTexts, 3);
    }

    /**
     * Упрощённый метод для inline клавиатуры 2 в ряд.
     */
    public static InlineKeyboardMarkup defaultInlineKeyboard(Map<String, String> buttonMap) {
        return createInlineKeyboardMarkup(buttonMap, 2);
    }
}
