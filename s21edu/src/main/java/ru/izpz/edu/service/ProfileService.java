package ru.izpz.edu.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.*;
import ru.izpz.edu.exception.ProfileNotFoundException;
import ru.izpz.edu.mapper.LastCommandConverter;
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

    private final ProfileMapper profileMapper;
    private final ProfileVerificationMapper profileVerificationMapper;
    private final ProfileRepository profileRepository;
    private final ProfileValidationRepository profileValidationRepository;
    private final CampusService campusService;
    private final ParticipantRepository participantRepository;
    private final ParticipantCampusRepository participantCampusRepository;

    public ProfileDto getOrCreateProfile(String telegramId) {
        return profileRepository.findByTelegramId(telegramId)
                .map(profileMapper::toDto)
                .orElseGet(() -> {
                    Profile profile = new Profile();
                    profile.setTelegramId(telegramId);
                    profile.setStatus(ProfileStatus.CREATED);
                    Profile saved = profileRepository.save(profile);
                    return profileMapper.toDto(saved);
                });
    }

    public ProfileDto getProfile(String telegramId) {
        return profileRepository.findByTelegramId(telegramId)
                .map(profileMapper::toDto)
                .orElseThrow(() -> new ProfileNotFoundException("Профиль не найден для telegramId = " + telegramId)
        );
    }

    public ProfileDto updateProfileStatus(ProfileRequest request) {
        return profileRepository.findByTelegramId(request.getTelegramId())
                .map(existing -> {
                    existing.setStatus(request.getStatus());
                    return profileRepository.save(existing);
                })
                .map(profileMapper::toDto)
                .orElseThrow(() -> new ProfileNotFoundException("Профиль не найден для telegramId = " + request.getTelegramId()));
    }

    public ProfileDto checkAndSetLogin(String telegramId, String s21login) {
        if (profileRepository.existsByS21login(s21login)) {
            throw new IllegalStateException("Логин " + s21login + " уже привязан к другому профилю");
        }
        Profile profile = profileRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new ProfileNotFoundException("Профиль не найден для telegramId = " + telegramId));
        if (profile.getS21login() != null) {
            throw new IllegalStateException("Профиль уже привязан к логину " + profile.getS21login());
        }
        profile.setS21login(s21login);
        return profileMapper.toDto(profileRepository.save(profile));
    }

    public ProfileCodeResponse getVerificationCode(String s21login) {
        ProfileValidation validation = profileValidationRepository.findByS21login(s21login).orElseGet(() -> {
            ProfileValidation profileValidation = new ProfileValidation();
            profileValidation.setS21login(s21login);
            profileValidation.setSecretCode(StringUtils.generateCode(4));
            profileValidation.setExpiresAt(OffsetDateTime.now());
            return profileValidationRepository.save(profileValidation);
        });

        return profileVerificationMapper.toProfileCodeResponse(validation);
    }

    public ParticipantDto getParticipant(String eduLogin) throws ApiException {
        var participantV1DTO = campusService.checkEduLogin(eduLogin);

        ParticipantCampus campus = profileMapper.toEntity(participantV1DTO.getCampus());
        participantCampusRepository.save(campus);

        Participant participant = profileMapper.toEntity(participantV1DTO);
        participant.setCampus(campus);
        participantRepository.save(participant);

        return profileMapper.toDto(participant);
    }

    public ProfileDto updateLastCommand(@Valid LastCommandRequest request) {
        Profile profile = profileRepository.findByTelegramId(request.getTelegramId())
                .orElseThrow(() -> new ProfileNotFoundException("Профиль не найден для telegramId = " + request.getTelegramId()));
        String serialized = request.getCommand() == null ? null : LastCommandConverter.serialize(request.getCommand());
        profile.setLastCommand(serialized);
        return profileMapper.toDto(profileRepository.save(profile));
    }
}
