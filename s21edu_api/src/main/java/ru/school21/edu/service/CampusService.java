package ru.school21.edu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.school21.edu.ApiException;
import ru.school21.edu.mapper.CampusMapper;
import ru.school21.edu.model.Coalition;
import ru.school21.edu.model.CoalitionV1DTO;
import ru.school21.edu.model.Participant;
import ru.school21.edu.model.ParticipantLoginsV1DTO;
import ru.school21.edu.repository.CampusRepository;
import ru.school21.edu.repository.CoalitionRepository;
import ru.school21.edu.repository.ParticipantRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CampusService {

    private final CampusApiProxy campusApi;
    private final CampusMapper campusMapper;
    private final CampusRepository campusRepository;
    private final ParticipantRepository participantRepository;
    private final CoalitionRepository coalitionRepository;

    public CampusService(CampusApiProxy campusApi,
                         CampusMapper campusMapper,
                         CampusRepository campusRepository,
                         ParticipantRepository participantRepository,
                         CoalitionRepository coalitionRepository,
                         @Value("${edu.tokenEndpoint}") String tokenEndpoint) {
        this.campusApi = campusApi;
        this.campusMapper = campusMapper;
        this.campusRepository = campusRepository;
        this.participantRepository = participantRepository;
        this.coalitionRepository = coalitionRepository;

        RestTemplate restTemplate = new RestTemplate();
        // Отправляем GET запрос к эндпоинту токена
        ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenEndpoint, String.class);
        String token = tokenResponse.getBody();
        if (token.isEmpty()) {
            throw new RuntimeException("Не удалось получить access token");
        }
        // Устанавливаем токен в ApiClient перед запросом:
        campusApi.getApiClient().setApiKey(token);
        campusApi.getApiClient().setApiKeyPrefix("Bearer");
    }

    @Scheduled(fixedDelay = 300000)
    public void startParsing() throws ApiException {
        getCampuses();
        getAllCoalitions();
        getAllParticipants();
    }

    public void getCampuses() throws ru.school21.edu.ApiException {
        var campuses = campusApi.getCampuses().getCampuses();
        for (var campus : campuses) {
            var entity = campusMapper.toEntity(campus);
            campusRepository.save(entity);
        }
    }

    public void getAllParticipants() throws ApiException {
        var campuses = campusRepository.findAll();
        for (var campus : campuses) {
            log.info("Обновление участников для кампуса: {}", campus.getId());
            getCampusParticipants(campus.getId());
        }
    }

    public void getAllCoalitions() throws ApiException {
        var campuses = campusRepository.findAll();
        for (var campus : campuses) {
            log.info("Обновление коалиций для кампуса: {}", campus.getId());
            getCampusCoalitions(campus.getId());
        }
    }

    public void getCampusCoalitions(String campusId) throws ApiException {
        log.info("Обновление коалиций для кампуса: {}", campusId);

        int offset = 0;
        final int limit = 1000;

        UUID campusIdStr = UUID.fromString(campusId);
        log.info("GET COALITIONS FOR CAMPUS {}", campusIdStr);

        // Получаем текущий бин из контекста (чтобы обойти циклическую зависимость)
        var response = campusApi.getCoalitionsByCampus(campusIdStr, limit, offset);
        if (response != null && !response.getCoalitions().isEmpty()) {
            List<CoalitionV1DTO> coalitions = response.getCoalitions();
            log.info("Получено {} коалиций для кампуса {} на странице с offset {}", coalitions.size(), campusIdStr, offset);

            // Для каждой полученной коалиции маппим в сущность и сохраняем
            for (CoalitionV1DTO coalition : coalitions) {
                log.info("Добавляем коалицию {} в кампус {}", coalition.getCoalitionId(), campusIdStr);
                Coalition coalitionEntity = new Coalition();
                coalitionEntity.setCoalitionId(coalition.getCoalitionId());
                coalitionEntity.setName(coalition.getName());
                coalitionEntity.setCampusId(campusId);
                coalitionRepository.save(coalitionEntity);
            }
        }
    }

    public void getCampusParticipants(String campusId) throws ApiException {
        log.info("Обновление участников для кампуса: {}", campusId);

        UUID campusIdStr = UUID.fromString(campusId);

        int offset = 0;
        final int limit = 1000;
        boolean hasMore = true;

        while (hasMore) {
            ParticipantLoginsV1DTO response = campusApi.getParticipantsByCampusId(campusIdStr, (long) limit, (long) offset);
            if (response != null && !response.getParticipants().isEmpty()) {
                List<String> logins = response.getParticipants();
                log.info("Получено {} участников для кампуса {} на странице с offset {}", logins.size(), campusIdStr, offset);

                // Для каждого полученного логина маппим в сущность и сохраняем
                for (String login : logins) {
                    log.info("Добавляем участника {} в кампус {}", login, campusIdStr);
                    Participant participant = new Participant();
                    participant.setLogin(login);
                    participant.setCampusId(campusId);
                    participantRepository.save(participant);
                }

                // Если получено ровно limit записей, то возможно есть следующая страница
                if (logins.size() == limit) {
                    offset += limit;
                } else {
                    hasMore = false;
                }
            } else {
                hasMore = false;
            }
        }
    }
}
