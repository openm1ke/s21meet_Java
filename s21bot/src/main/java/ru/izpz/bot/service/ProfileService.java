package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.bot.client.ProfileClient;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.model.ErrorResponseDTO;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.utils.FeignErrorParser;

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

    public ParticipantV1DTO checkEduLogin(String login) {
        log.info("Получен запрос на проверку логина: login = {}", login);
        try {
            return profileClient.checkEduLogin(login);
        } catch (FeignException e) {
            ErrorResponseDTO error = FeignErrorParser.parse(e);
            log.warn("Ошибка логина: {}", error.getMessage());

            // Пробрасываем с кастомным исключением, если нужно
            throw new EduLoginCheckException(error);
        }
    }

    public ProfileDto updateProfileStatus(Long chatId, ProfileStatus status) {
        log.info("Обновление статуса профиля {} на {}", chatId, status);
        ProfileRequest profileRequest = ProfileRequest.builder()
                .telegramId(chatId.toString())
                .status(status)
                .build();
        System.out.println(profileRequest);
        try {
            return profileClient.updateProfileStatus(profileRequest);
        } catch (FeignException e) {
            log.error("Ошибка обновления профиля {}", e.contentUTF8());
            throw e;
        }
    }

    public ProfileDto checkAndSetLogin(Long chatId, String login) {
        log.info("Сохранение логина профиля {} для {}", chatId, login);
        ProfileRequest profileRequest = ProfileRequest.builder()
                .telegramId(chatId.toString())
                .s21login(login)
                .build();
        try {
            return profileClient.checkAndSetLogin(profileRequest);
        } catch (FeignException e) {
            log.error("Ошибка обновления профиля {}", e.contentUTF8());
            throw e;
        }
    }
}
