package ru.izpz.bot.keyboard;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.dto.FriendDto;
import ru.izpz.dto.FriendsSliceDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    public static InlineKeyboardMarkup createUrlKeyboard(Map<String, String> buttons, int rowSize) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (Map.Entry<String, String> entry : buttons.entrySet()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(entry.getKey())
                .url(entry.getValue())
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

    public static InlineKeyboardMarkup friendsListKeyboard(FriendsSliceDto friends, int rowSize, int page) {
        Map<String, String> buttons = new LinkedHashMap<>();

        List<FriendDto> content = friends.content();

        for (int i = 0; i < content.size(); i++) {
            FriendDto friend = content.get(i);
            int ordinal = i + 1;
            buttons.put(
                String.valueOf(ordinal),
                CallbackPayloadSerializer.serialize(
                        new CallbackPayload("show_friend", Map.of("login", friend.getLogin()))
                )
            );
        }

        // блок навигации
        if (page > 0) {
            buttons.put(
                "◀ Назад",
                CallbackPayloadSerializer.serialize(
                        new CallbackPayload("friends_page", Map.of("page", String.valueOf(page - 1)))
                )
            );
        }

        if (friends.hasNext()) { // использовать slice.hasNext()
            buttons.put(
                "Вперёд ▶",
                CallbackPayloadSerializer.serialize(
                        new CallbackPayload("friends_page", Map.of("page", String.valueOf(page + 1)))
                )
            );
        }

        return createInlineKeyboardMarkup(buttons, rowSize);
    }

    public static String friendsListText(FriendsSliceDto friends, int page) {
        StringBuilder sb = new StringBuilder();
        sb.append("Друзья (стр. ").append(page + 1).append(")\n\n");

        List<FriendDto> content = friends.content();

        for (int i = 0; i < content.size(); i++) {
            FriendDto f = content.get(i);
            int ordinal = i + 1; // локальная нумерация на странице
            sb.append(ordinal)
                    .append(". ")
                    .append(f.getLogin());
            sb.append("\n");
        }

        if (content.isEmpty()) {
            sb.append("Список пуст");
        }

        return sb.toString();
    }

    public static AnswerCallbackQuery createAnswerCallbackQuery(String callbackId, String text, boolean showAlert) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackId);
        answerCallbackQuery.setText(text);
        answerCallbackQuery.setShowAlert(showAlert);
        return answerCallbackQuery;
    }

    public static InlineKeyboardMarkup getFriendInlineKeyboard(FriendDto friend) {
        var login = friend.getLogin();
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        var isFriendLabel = friend.getIsFriend() ? "Удалить из друзей" : "Добавить в друзья";
        var isFavoriteLabel = friend.getIsFavorite() ? "Удалить из избранного" : "Добавить в избранное";
        var isSubscribedLabel = friend.getIsSubscribe() ? "Отписаться" : "Подписаться";
        var setNameLabel = friend.getName() == null ? "Указать имя" : "Изменить имя";

        data.put(isFriendLabel, CallbackPayloadSerializer.serialize(new CallbackPayload("add_friend", Map.of("login", login))));
        if (friend.getIsFriend()) {
            data.put(setNameLabel, CallbackPayloadSerializer.serialize(new CallbackPayload("set_name", Map.of("login", login))));
            data.put(isSubscribedLabel, CallbackPayloadSerializer.serialize(new CallbackPayload("subscribe", Map.of("login", login))));
            data.put(isFavoriteLabel, CallbackPayloadSerializer.serialize(new CallbackPayload("favorite", Map.of("login", login))));
        }
        return TelegramKeyboardFactory.defaultInlineKeyboard(data);
    }

    public static EditMessageReplyMarkup editFriendInlineKeyboard(InlineKeyboardMarkup keyboard, Long chatId, int messageId) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(chatId);
        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setReplyMarkup(keyboard);
        return editMessageReplyMarkup;
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
