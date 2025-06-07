package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.bot.client.ProfileClient;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.ProfileStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileClient profileClient;

    public ProfileDto getProfile(Long chatId) {
        log.info("Получение профиля {}", chatId);
        try {
            return profileClient.getOrCreateProfile(chatId.toString());
        } catch (FeignException e) {
            log.error("Ошибка обработки профиля", e);
            throw e;
        }
    }

    public void checkEduLogin(String login) {
        try {
            profileClient.checkEduLogin(login);
        } catch (FeignException e) {
            log.error("Ошибка обработки профиля", e);
            throw e;
        }
    }

    public ProfileDto updateProfileStatus(Long chatId, ProfileStatus status) {
        ProfileRequest profileRequest = ProfileRequest.builder()
                .telegramId(chatId.toString())
                .status(status)
                .build();
        try {
            return profileClient.updateProfileStatus(profileRequest);
        } catch (FeignException e) {
            log.error("Ошибка обновления профиля {}", e.contentUTF8());
            throw e;
        }
    }
}
