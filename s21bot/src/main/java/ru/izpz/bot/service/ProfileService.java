package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.bot.client.ProfileClient;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileClient profileClient;

    public ProfileDto getProfile(Long chatId) {
        var profileRequest = ProfileRequest.builder()
                //.telegramId("1234")
                .telegramId(chatId.toString())
                .build();
        log.info("ProfileRequest: {}", profileRequest.toString());
        try {
            return profileClient.getOrCreateProfile(profileRequest);
        } catch (FeignException e) {
            log.error("Ошибка обработки профиля", e);
            throw e;
        }
    }
}
