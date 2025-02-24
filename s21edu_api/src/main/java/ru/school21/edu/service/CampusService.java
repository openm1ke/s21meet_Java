package ru.school21.edu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.school21.edu.api.CampusApi;
import ru.school21.edu.mapper.CampusMapper;
import ru.school21.edu.repository.CampusRepository;

@Service
public class CampusService {

    private final CampusApi campusApi;
    private final CampusMapper campusMapper;
    private final CampusRepository campusRepository;
    private final String tokenEndpoint;

    public CampusService(CampusApi campusApi,
                         CampusMapper campusMapper,
                         CampusRepository campusRepository,
                         @Value("${edu.tokenEndpoint}") String tokenEndpoint) {
        this.campusApi = campusApi;
        this.campusMapper = campusMapper;
        this.campusRepository = campusRepository;
        this.tokenEndpoint = tokenEndpoint;
    }

    @Scheduled(fixedDelay = 300000)
    public void getCampuses() throws ru.school21.edu.ApiException {
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

        var campuses = campusApi.getCampusesWithHttpInfo();

        for (var campus : campuses.getData().getCampuses()) {
            var entity = campusMapper.toEntity(campus);
            campusRepository.save(entity);
        }
    }
}
