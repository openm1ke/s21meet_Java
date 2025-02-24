package ru.school21.edu.service;

import edu.service.TokenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.school21.edu.api.CampusApi;
import ru.school21.edu.mapper.CampusMapper;
import ru.school21.edu.repository.CampusRepository;

@Service
public class CampusService {

    private final CampusApi campusApi;
    private final CampusMapper campusMapper;
    private final CampusRepository campusRepository;
    private final TokenService tokenService;

    public CampusService(CampusApi campusApi,
                         CampusMapper campusMapper,
                         CampusRepository campusRepository,
                         TokenService tokenService) {
        this.campusApi = campusApi;
        this.campusMapper = campusMapper;
        this.campusRepository = campusRepository;
        this.tokenService = tokenService;
    }

    @Scheduled(fixedDelay = 300000)
    public void getCampuses() throws ru.school21.edu.ApiException {
        String token = tokenService.getDefaultAccessToken();
        if(token == null) {
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
