package ru.izpz.bot.keyboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.dto.EventDto;
import ru.izpz.dto.EventsSliceDto;
import ru.izpz.dto.FriendsSliceDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ListKeyboardFactory {

    private final CallbackPayloadSerializer serializer;

    public <T> InlineKeyboardMarkup createListKeyboard(
            List<T> items,
            int rowSize,
            int page,
            boolean hasNext,
            Function<T, Map.Entry<String, String>> itemMapper,
            String pageCallbackType
    ) {
        Map<String, String> buttons = new LinkedHashMap<>();

        // Добавляем элементы списка
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            int ordinal = i + 1;
            Map.Entry<String, String> buttonData = itemMapper.apply(item);
            buttons.put(String.valueOf(ordinal), buttonData.getValue());
        }

        // Добавляем навигацию
        addNavigationButtons(buttons, page, hasNext, pageCallbackType);

        return createInlineKeyboardMarkup(buttons, rowSize);
    }

    public <T> String createListText(
            List<T> items,
            String title,
            Function<T, String> itemFormatter
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n\n");

        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            int ordinal = i + 1;
            sb.append(ordinal)
                    .append(". ")
                    .append(itemFormatter.apply(item))
                    .append("\n");
        }

        if (items.isEmpty()) {
            sb.append("Список пуст");
        }

        return sb.toString();
    }

    private void addNavigationButtons(
            Map<String, String> buttons,
            int page,
            boolean hasNext,
            String callbackType
    ) {
        // Кнопка "Назад"
        if (page > 0) {
            buttons.put(
                    "◀ Назад",
                    serializer.serialize(
                            new CallbackPayload(callbackType, Map.of("page", String.valueOf(page - 1)))
                    )
            );
        }

        // Кнопка "Вперёд"
        if (hasNext) {
            buttons.put(
                    "Вперёд ▶",
                    serializer.serialize(
                            new CallbackPayload(callbackType, Map.of("page", String.valueOf(page + 1)))
                    )
            );
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(Map<String, String> buttons, int rowSize) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (Map.Entry<String, String> entry : buttons.entrySet()) {
            var button = org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.builder()
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

    // Специфичные методы для событий
    public InlineKeyboardMarkup eventsListKeyboard(EventsSliceDto events, int rowSize, int page) {
        return createListKeyboard(
                events.content(),
                rowSize,
                page,
                events.hasNext(),
                event -> Map.entry(
                        event.name(),
                        serializer.serialize(new CallbackPayload("event", Map.of("id", event.id().toString())))
                ),
                "events_page"
        );
    }

    // Специфичные методы для друзей
    public InlineKeyboardMarkup friendsListKeyboard(FriendsSliceDto friends, int rowSize, int page) {
        return createListKeyboard(
                friends.content(),
                rowSize,
                page,
                friends.hasNext(),
                friend -> Map.entry(
                        friend.getLogin(),
                        serializer.serialize(new CallbackPayload("show_friend", Map.of("login", friend.getLogin())))
                ),
                "friends_page"
        );
    }

    // Специфичные методы для текста
    public String eventsListText(EventsSliceDto events) {
        return createListText(
                events.content(),
                "\uD83D\uDE38 События в кампусе \uD83D\uDE3D",
                EventDto::name
        );
    }

    public String friendsListText(FriendsSliceDto friends) {
        return createListText(
                friends.content(),
                "\uD83D\uDE38 Мои друзья \uD83D\uDE3D",
                this::formatFriend
        );
    }

    private String formatFriend(ru.izpz.dto.FriendDto friend) {
        StringBuilder sb = new StringBuilder();
        sb.append(friend.getLogin());
        
        if (friend.getName() != null && !friend.getName().isBlank()) {
            sb.append(" (").append(friend.getName()).append(")");
        }
        
        if (friend.getIsFavorite()) sb.append("⭐");
        if (friend.getIsSubscribe()) sb.append("\uD83D\uDD14");
        sb.append(friend.getStatus() != null ? friend.getStatus().getEmoji() : "❓");
        
        if (friend.getIsOnline()) {
            sb.append("\uD83D\uDFE2");
            sb.append(" ").append(friend.getClusterName()).append("-").append(friend.getRow()).append(friend.getNumber());
        } else {
            sb.append("\uD83D\uDCA4");
        }
        
        return sb.toString();
    }
}
