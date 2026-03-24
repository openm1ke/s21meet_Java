package ru.izpz.edu.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.izpz.dto.*;
import ru.izpz.dto.CampusDto;
import ru.izpz.dto.ParticipantSeatDto;
import ru.izpz.edu.exception.EntityNotFoundException;
import ru.izpz.edu.mapper.ProfileMapper;
import ru.izpz.edu.mapper.ProfileVerificationMapper;
import ru.izpz.edu.model.Participant;
import ru.izpz.edu.model.Profile;
import ru.izpz.edu.model.ProfileValidation;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.repository.OnlineRepository;
import ru.izpz.edu.repository.ProfileRepository;
import ru.izpz.edu.repository.ProfileValidationRepository;
import ru.izpz.edu.repository.WorkplaceRepository;
import ru.izpz.edu.utils.StringUtils;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class ProfileService {

    private static final String PROFILE_NOT_FOUND_MESSAGE = "Профиль не найден для telegramId = ";
    private static final String DEFAULT_CAMPUS_ID = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
    private static final String DEFAULT_CAMPUS_NAME = "Moscow";

    private final ProfileMapper profileMapper;
    private final ProfileVerificationMapper profileVerificationMapper;
    private final ParticipantSyncService participantSyncService;
    private final ParticipantCoalitionService participantCoalitionService;
    private final ProfileRepository profileRepository;
    private final ProfileValidationRepository profileValidationRepository;
    private final WorkplaceRepository workplaceRepository;
    private final OnlineRepository onlineRepository;
    private final ClusterRepository clusterRepository;

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
        Profile profile = profileRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new EntityNotFoundException(PROFILE_NOT_FOUND_MESSAGE + telegramId));
        if (profile.getS21login() != null) {
            if (profile.getS21login().equals(s21login)) {
                return profileMapper.toDto(profile);
            }
            throw new IllegalStateException("Профиль уже привязан к логину " + profile.getS21login());
        }
        if (profileRepository.existsByS21login(s21login)) {
            throw new IllegalStateException("Логин " + s21login + " уже привязан к другому профилю");
        }
        profile.setS21login(s21login);
        try {
            ProfileDto savedProfile = profileMapper.toDto(profileRepository.save(profile));
            warmUpParticipantData(s21login);
            return savedProfile;
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition in checkAndSetLogin for telegramId={}, s21login={}: {}",
                telegramId, s21login, e.getMessage());
            ProfileDto profileDto = profileRepository.findByTelegramId(telegramId)
                .map(profileMapper::toDto)
                .orElseGet(() -> profileMapper.toDto(profile));
            if (s21login.equals(profileDto.s21login())) {
                warmUpParticipantData(s21login);
            }
            return profileDto;
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
        Participant participant = participantSyncService.getOrSyncByEduLogin(eduLogin);

        ParticipantDto dto = profileMapper.toDto(participant);
        participantCoalitionService.enrichParticipant(dto, eduLogin);
        enrichPresence(dto, eduLogin);
        return dto;
    }

    public ProfileDto updateLastCommand(@Valid LastCommandRequest request) {
        Profile profile = profileRepository.findByTelegramId(request.getTelegramId())
                .orElseThrow(() -> new EntityNotFoundException(PROFILE_NOT_FOUND_MESSAGE + request.getTelegramId()));
        profile.setLastCommand(request.getCommand());
        return profileMapper.toDto(profileRepository.save(profile));
    }

    public ParticipantDto checkEduLogin(String login) throws ApiException {
        log.info("Получен запрос на проверку логина: login = {}", login);
        return profileMapper.toDto(participantSyncService.fetchByEduLogin(login));
    }

    public CampusDto getCampus(String telegramId) {
        log.info("Получен запрос на получение кампуса для telegramId = {}", telegramId);
        var profile = profileRepository.findByTelegramId(telegramId);
        if (profile.isEmpty()) {
            throw new EntityNotFoundException("Не найден логин для данного телеграм айди");
        }
        String s21login = profile.get().getS21login();
        if (s21login == null || s21login.isBlank()) {
            log.warn("Не найден s21login для telegramId = {}, используем кампус по умолчанию {}", telegramId, DEFAULT_CAMPUS_NAME);
            return defaultCampus();
        }

        try {
            Participant participant = participantSyncService.getOrSyncByEduLogin(s21login);
            if (participant.getCampus() != null) {
                return toCampusDto(participant.getCampus().getCampusName(), participant.getCampus().getId());
            }
            log.warn("Не удалось определить кампус пользователя {}, используем кампус по умолчанию {}", s21login, DEFAULT_CAMPUS_NAME);
            return defaultCampus();
        } catch (ApiException | IllegalStateException e) {
            log.warn("Ошибка получения кампуса для {}, используем кампус по умолчанию {}", s21login, DEFAULT_CAMPUS_NAME, e);
            return defaultCampus();
        }
    }

    private CampusDto defaultCampus() {
        return new CampusDto(DEFAULT_CAMPUS_NAME, DEFAULT_CAMPUS_ID);
    }

    private CampusDto toCampusDto(String campusName, String campusId) {
        String normalizedName = campusName;
        if (normalizedName == null || normalizedName.isBlank()) {
            normalizedName = DEFAULT_CAMPUS_NAME;
        }
        return new CampusDto(normalizedName, campusId);
    }

    private void warmUpParticipantData(String s21login) {
        try {
            participantSyncService.syncByEduLogin(s21login);
        } catch (ApiException | RuntimeException e) {
            log.warn("Не удалось прогреть данные участника {}: {}", s21login, e.getMessage());
        }
        participantCoalitionService.refreshByLogin(s21login);
    }

    private void enrichPresence(ParticipantDto dto, String login) {
        Workplace seat = workplaceRepository.findByLogin(login).orElse(null);
        if (seat != null) {
            dto.setIsOnline(true);
            ParticipantSeatDto seatDto = new ParticipantSeatDto();
            seatDto.setStageGroupName(seat.getStageGroupName());
            seatDto.setStageName(seat.getStageName());
            if (seat.getId() != null) {
                seatDto.setRow(seat.getId().getRow());
                seatDto.setNumber(seat.getId().getNumber());
                Long clusterId = seat.getId().getClusterId();
                if (clusterId != null) {
                    clusterRepository.findById(clusterId).ifPresent(cluster -> seatDto.setClusterName(cluster.getName()));
                }
            }
            dto.setSeat(seatDto);
            dto.setLastSeenAt(null);
            return;
        }

        dto.setIsOnline(false);
        dto.setSeat(null);
        onlineRepository.findByLogin(login).ifPresent(online -> dto.setLastSeenAt(online.getLastSeenAt()));
    }
}
