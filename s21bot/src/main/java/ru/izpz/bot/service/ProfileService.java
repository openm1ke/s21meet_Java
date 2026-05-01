package ru.izpz.bot.service;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ru.izpz.bot.client.ProfileClient;
import ru.izpz.bot.client.RocketChatClient;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.RocketChatSendException;
import ru.izpz.dto.*;
import ru.izpz.utils.FeignErrorParser;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
  private static final String PROFILE_EDU = "profileEdu";

  private final ProfileClient profileClient;
  private final RocketChatClient rocketChatClient;

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public ProfileDto getProfile(Long chatId) {
    log.info("Получение профиля {}", chatId);
    try {
      return profileClient.getOrCreateProfile(chatId.toString());
    } catch (FeignException e) {
      log.error("Ошибка обработки профиля для {}", chatId, e);
      throw e;
    }
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public ParticipantDto checkEduLogin(String login) {
    log.info("Получен запрос на проверку логина: login = {}", login);
    try {
      return profileClient.checkEduLogin(login);
    } catch (FeignException e) {
      ServiceErrorDto error = FeignErrorParser.parse(e);
      log.warn("Ошибка логина: {}", error.getMessage());

      // Пробрасываем с кастомным исключением, если нужно
      throw new EduLoginCheckException(error);
    }
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public ProfileDto updateProfileStatus(Long chatId, ProfileStatus status) {
    log.info("Обновление статуса профиля {} на {}", chatId, status);
    ProfileRequest profileRequest =
        ProfileRequest.builder().telegramId(chatId.toString()).status(status).build();
    try {
      return profileClient.updateProfileStatus(profileRequest);
    } catch (FeignException e) {
      log.error("Ошибка обновления статуса профиля {}", e.contentUTF8());
      throw e;
    }
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public ProfileDto checkAndSetLogin(Long chatId, String login) {
    log.info("Сохранение логина профиля {} для {}", chatId, login);
    ProfileRequest profileRequest =
        ProfileRequest.builder().telegramId(chatId.toString()).s21login(login).build();
    try {
      return profileClient.checkAndSetLogin(profileRequest);
    } catch (FeignException e) {
      log.error("Ошибка обновления профиля {}", e.contentUTF8());
      throw e;
    }
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public ProfileCodeResponse getVerificationCode(String s21login) {
    ProfileCodeRequest request = ProfileCodeRequest.builder().s21login(s21login).build();
    try {
      return profileClient.getProfileCode(request);
    } catch (FeignException e) {
      log.error("Ошибка генерации кода подтверждения {}", e.contentUTF8());
      throw e;
    }
  }

  public RocketChatSendResponse sendVerificationCode(String s21login) {
    try {
      ProfileCodeResponse code = getVerificationCode(s21login);
      String message =
          String.format("Привет! Вот твой код подтверждения: %s", code.getSecretCode());
      RocketChatSendRequest rocketChatSendRequest =
          RocketChatSendRequest.builder().username(code.getS21login()).message(message).build();
      var response = rocketChatClient.sendMessage(rocketChatSendRequest);
      if (response.isSuccess()) {
        return response;
      } else {
        throw new RocketChatSendException(response);
      }
    } catch (FeignException e) {
      log.error("Ошибка отправки кода подтверждения {}", e.contentUTF8());
      throw e;
    }
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public CampusResponse showCampusMap(Long chatId) {
    var request = CampusRequest.builder().telegramId(chatId.toString()).build();
    try {
      return profileClient.getCampusMap(request);
    } catch (FeignException e) {
      log.error("Ошибка получения карты кампуса {}", e.contentUTF8());
      throw e;
    }
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public ParticipantDto showParticipant(String telegramId, String eduLogin) {
    var request = ParticipantRequest.builder().telegramId(telegramId).eduLogin(eduLogin).build();
    try {
      return profileClient.getParticipant(request);
    } catch (FeignException e) {
      log.error("Ошибка получения профиля {}", e.contentUTF8());
      throw e;
    }
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public void setLastCommand(Long chatId, LastCommandState command) {
    profileClient.setLastCommand(new LastCommandRequest(chatId.toString(), command));
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public FriendDto applyFriend(
      Long telegramId, String login, FriendRequest.Action action, @Nullable String name) {
    var builder =
        FriendRequest.builder().telegramId(telegramId.toString()).action(action).login(login);
    if (name != null) {
      builder.name(name);
    }
    return profileClient.applyFriend(builder.build());
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public FriendsSliceDto getFriends(Long chatId, int page, int pageSize) {
    return profileClient.getFriends(chatId.toString(), page, pageSize);
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public EventDto getEvent(long eventId) {
    return profileClient.getEvent(eventId);
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public EventsSliceDto getEvents(Long chatId, int page, int pageSize) {
    return profileClient.getEvents(chatId.toString(), page, pageSize);
  }

  @Retry(name = PROFILE_EDU)
  @CircuitBreaker(name = PROFILE_EDU)
  public List<ProjectsDto> getProjects(String login) {
    return profileClient.getProjects(login);
  }
}
