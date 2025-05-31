package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    @Value("${bot.admin}")
    private Long ADMIN_ID;

    private final OkHttpTelegramClient telegramClient;
    private final ProfileService profileService;

    public void process(Message message) {
        Chat chat = message.getChat();
        Long chatId = message.getChatId();
        ProfileDto profile;
        try {
            var profileRequest = ProfileRequest.builder()
                    .telegramId(chatId.toString())
                    //.telegramId("1234")
                    .build();
            log.info("ProfileRequest: {}", profileRequest.toString());
            profile = profileService.getProfile(profileRequest);
            log.info("Profile: {}", profile.toString());

            log.info("{} написал {}", chat.getUserName(), message.getText());
            sendMessage(chatId, profile.toString());
        } catch (FeignException e) {
            sendMessage(chatId, "Ошибка обработки профиля");
            sendMessage(ADMIN_ID, e.contentUTF8());
            throw e;
        }
    }

    public void sendMessage(Long chatId, String message) {
        SendMessage response = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .build();

        try {
            telegramClient.execute(response);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения для {}: {}", chatId, message, e);
        }
    }
}
