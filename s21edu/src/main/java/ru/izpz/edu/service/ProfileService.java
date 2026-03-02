package ru.izpz.edu.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.izpz.dto.*;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.dto.CampusDto;
import ru.izpz.edu.exception.EntityNotFoundException;
import ru.izpz.edu.mapper.ProfileMapper;
import ru.izpz.edu.mapper.ProfileVerificationMapper;
import ru.izpz.edu.model.Participant;
import ru.izpz.edu.model.ParticipantCampus;
import ru.izpz.edu.model.Profile;
import ru.izpz.edu.model.ProfileValidation;
import ru.izpz.edu.repository.ParticipantCampusRepository;
import ru.izpz.edu.repository.ParticipantRepository;
import ru.izpz.edu.repository.ProfileRepository;
import ru.izpz.edu.repository.ProfileValidationRepository;
import ru.izpz.edu.utils.StringUtils;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class ProfileService {

    private static final String PROFILE_NOT_FOUND_MESSAGE = "Профиль не найден для telegramId = ";

    private final ProfileMapper profileMapper;
    private final ProfileVerificationMapper profileVerificationMapper;
    private final ProfileRepository profileRepository;
    private final ProfileValidationRepository profileValidationRepository;
    private final ParticipantApi participantApi;
    private final ParticipantRepository participantRepository;
    private final ParticipantCampusRepository participantCampusRepository;

    public ProfileDto getOrCreateProfile(String telegramId) {
        return profileRepository.findByTelegramId(telegramId)
            .map(profileMapper::toDto)
            .orElseGet(() -> {
                Profile profile = new Profile();
                profile.setTelegramId(telegramId);
                profile.setStatus(ProfileStatus.CREATED);
                try {
                    Profile saved = profileRepository.save(profile);
                    return profileMapper.toDto(saved);
                } catch (DataIntegrityViolationException e) {
                    log.warn("Race condition in getOrCreateProfile for telegramId={}: {}", telegramId, e.getMessage());
                    return profileRepository.findByTelegramId(telegramId)
                        .map(profileMapper::toDto)
                        .orElseGet(() -> profileMapper.toDto(profile));
                }
            });
    }

    public ProfileDto getProfile(String telegramId) {
        return profileRepository.findByTelegramId(telegramId)
            .map(profileMapper::toDto)
            .orElseThrow(() -> new EntityNotFoundException(PROFILE_NOT_FOUND_MESSAGE + telegramId)
        );
    }

    public ProfileDto updateProfileStatus(ProfileRequest request) {
        return profileRepository.findByTelegramId(request.getTelegramId())
            .map(existing -> {
                existing.setStatus(request.getStatus());
                return profileRepository.save(existing);
            })
            .map(profileMapper::toDto)
            .orElseThrow(() -> new EntityNotFoundException(PROFILE_NOT_FOUND_MESSAGE + request.getTelegramId()));
    }

    public ProfileDto checkAndSetLogin(String telegramId, String s21login) {
        if (profileRepository.existsByS21login(s21login)) {
            throw new IllegalStateException("Логин " + s21login + " уже привязан к другому профилю");
        }
        Profile profile = profileRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new EntityNotFoundException(PROFILE_NOT_FOUND_MESSAGE + telegramId));
        if (profile.getS21login() != null) {
            throw new IllegalStateException("Профиль уже привязан к логину " + profile.getS21login());
        }
        profile.setS21login(s21login);
        try {
            return profileMapper.toDto(profileRepository.save(profile));
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition in checkAndSetLogin for telegramId={}, s21login={}: {}",
                telegramId, s21login, e.getMessage());
            return profileRepository.findByTelegramId(telegramId)
                .map(profileMapper::toDto)
                .orElseGet(() -> profileMapper.toDto(profile));
        }
    }

    public ProfileCodeResponse getVerificationCode(String s21login) {
        ProfileValidation validation = profileValidationRepository.findByS21login(s21login).orElseGet(() -> {
            ProfileValidation profileValidation = new ProfileValidation();
            profileValidation.setS21login(s21login);
            profileValidation.setSecretCode(StringUtils.generateCode(4));
            profileValidation.setExpiresAt(OffsetDateTime.now());
            try {
                return profileValidationRepository.save(profileValidation);
            } catch (DataIntegrityViolationException e) {
                log.warn("Race condition while creating verification code for s21login={}: {}", s21login, e.getMessage());
                return profileValidationRepository.findByS21login(s21login)
                    .orElse(profileValidation);
            }
        });

        return profileVerificationMapper.toProfileCodeResponse(validation);
    }

    public ParticipantDto getParticipant(String eduLogin) throws ApiException {
        var participantV1DTO = checkEduLogin(eduLogin);

        ParticipantCampus campus = profileMapper.toEntity(participantV1DTO.getCampus());
        participantCampusRepository.save(campus);

        Participant participant = profileMapper.toEntity(participantV1DTO);
        participant.setCampus(campus);
        participantRepository.save(participant);

        return profileMapper.toDto(participant);
    }

    public ProfileDto updateLastCommand(@Valid LastCommandRequest request) {
        Profile profile = profileRepository.findByTelegramId(request.getTelegramId())
                .orElseThrow(() -> new EntityNotFoundException(PROFILE_NOT_FOUND_MESSAGE + request.getTelegramId()));
        profile.setLastCommand(request.getCommand());
        return profileMapper.toDto(profileRepository.save(profile));
    }

    public ParticipantV1DTO checkEduLogin(String login) throws ApiException {
        log.info("Получен запрос на проверку логина: login = {}", login);
        return participantApi.getParticipantByLogin(login);
    }

    public CampusDto getCampus(String telegramId) throws ApiException {
        log.info("Получен запрос на получение кампуса для telegramId = {}", telegramId);
        var profile = profileRepository.findByTelegramId(telegramId);
        if (profile.isEmpty()) {
            throw new EntityNotFoundException("Не найден логин для данного телеграм айди");
        }
        var participant = checkEduLogin(profile.get().getS21login());
        return new CampusDto(participant.getCampus().getShortName(), participant.getCampus().getId().toString());
    }
}
