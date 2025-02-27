package ru.school21.edu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.school21.edu.ApiClient;
import ru.school21.edu.ApiException;
import ru.school21.edu.mapper.CampusMapper;
import ru.school21.edu.model.*;
import ru.school21.edu.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class CampusService {

    private final CampusApiProxy campusApi;
    private final ClusterApiProxy clusterApi;
    private final CampusMapper campusMapper;
    private final CampusRepository campusRepository;
    private final ParticipantRepository participantRepository;
    private final CoalitionRepository coalitionRepository;
    private final ClusterRepository clusterRepository;
    private final WorkplaceRepository workplaceRepository;
    private final ApiClient apiClient;
    private final TokenService tokenService;

    public CampusService(CampusApiProxy campusApi,
                         ClusterApiProxy clusterApi,
                         CampusMapper campusMapper,
                         CampusRepository campusRepository,
                         ParticipantRepository participantRepository,
                         CoalitionRepository coalitionRepository,
                         ClusterRepository clusterRepository,
                         WorkplaceRepository workplaceRepository,
                         ApiClient apiClient, TokenService tokenService) {
        this.campusApi = campusApi;
        this.clusterApi = clusterApi;
        this.campusMapper = campusMapper;
        this.campusRepository = campusRepository;
        this.participantRepository = participantRepository;
        this.coalitionRepository = coalitionRepository;
        this.clusterRepository = clusterRepository;
        this.workplaceRepository = workplaceRepository;
        this.apiClient = apiClient;
        this.tokenService = tokenService;
    }

    @Scheduled(fixedDelay = 300000)
    public void startParsing() throws ApiException {
        // каждый раз обновляем токен на актуальный для всех клиентов
        apiClient.setApiKey(tokenService.getToken());

        boolean result = getAllCampuses(); // получаем uuid всех кампусов
        if (result) {
            log.info("Кампусы обновлены");

            CompletableFuture<Void> coalitionsFuture = getAllCoalitions(); // по ним парсим названия трайбов
            CompletableFuture<Void> participantsFuture = getAllParticipants(); // так же по кампусам получаем логины всех пиров
            CompletableFuture<Void> clustersFuture = getAllClusters(); // далее в каждом кампусе парсим кластеры
            CompletableFuture<Void> workplacesFutureByClusters = clustersFuture.thenCompose(v -> {
                try {
                    return getAllParticipantsByCluster();
                } catch (ApiException e) {
                    return CompletableFuture.failedFuture(e);
                }
            }); // и теперь по каждому кампусу получаем занятые места по кластерам

            CompletableFuture.allOf(coalitionsFuture, participantsFuture, clustersFuture, workplacesFutureByClusters).join();
        }
        log.info("Обновление всех данных для кампусов завершено");
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> getAllParticipantsByCluster() throws ApiException {
        var clusters = clusterRepository.findAll();
        for (var cluster : clusters) {
            getParticipantsByCluster(cluster.getClusterId());
        }
        log.info("Данные участников по кластерам обновлены.");
        return CompletableFuture.completedFuture(null);
    }

    public void getParticipantsByCluster(Long clusterId) throws ApiException {

        log.info("Получение списка занятых рабочих мест по кластерам {}", clusterId);
        int offset = 0;
        final int limit = 1000;

        var response = clusterApi.getParticipantsByCoalitionId1(clusterId, limit, offset, null);
        if(response != null && !response.getClusterMap().isEmpty()) {
            var clusterMap = response.getClusterMap();
            log.info("Получено {} участников для кластера {} на странице с offset {}", clusterMap.size(), clusterId, offset);
            // Для каждого полученного логина маппим в сущность и сохраняем
            ArrayList<Workplace> workplaces = new ArrayList<>();
            for (var workplace : clusterMap) {
                log.info("Добавляем участника {} из кластера {}", workplace.getLogin(), clusterId);
                WorkplaceId workplaceId = new WorkplaceId(clusterId, workplace.getRow(), workplace.getNumber());
                Workplace workplaceEntity = new Workplace();
                workplaceEntity.setId(workplaceId);
                workplaceEntity.setLogin(workplace.getLogin());
                workplaces.add(workplaceEntity);
            }
            log.info("Сохраняем {} участников для кластера {}", workplaces.size(), clusterId);
            workplaceRepository.saveAllAndFlush(workplaces);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> getAllClusters() throws ApiException {
        var campuses = campusRepository.findAll();
        for (var campus : campuses) {
            getClustersByCampus(UUID.fromString(campus.getId()));
        }
        log.info("Кластеры обновлены");
        return CompletableFuture.completedFuture(null);
    }

    public void getClustersByCampus(UUID campusId) throws ApiException {
        log.info("Получение списка кластеров для кампуса {}", campusId);
        var clusters = campusApi.getClustersByCampus(campusId).getClusters();
        ArrayList<Cluster> clusterEntities = new ArrayList<>();
        for (var cluster : clusters) {
            Cluster clusterEntity = new Cluster();
            clusterEntity.setClusterId(cluster.getId());
            clusterEntity.setName(cluster.getName());
            clusterEntity.setCapacity(cluster.getCapacity());
            clusterEntity.setAvailableCapacity(cluster.getAvailableCapacity());
            clusterEntity.setFloor(cluster.getFloor());
            clusterEntity.setCampusId(campusId.toString());
            clusterEntities.add(clusterEntity);
        }
        log.info("Сохраняем {} кластеров для кампуса {}", clusterEntities.size(), campusId);
        clusterRepository.saveAllAndFlush(clusterEntities);
    }

    public boolean getAllCampuses() throws ApiException {
        var campuses = campusApi.getCampuses().getCampuses();
        ArrayList<Campus> campusEntities = new ArrayList<>();
        for (var campus : campuses) {
            var entity = campusMapper.toEntity(campus);
            campusEntities.add(entity);
        }
        // сохраняем все кампусы в базу и сразу делаем записи доступными для чтения
        campusRepository.saveAllAndFlush(campusEntities);
        return true;
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> getAllParticipants() throws ApiException {
        var campuses = campusRepository.findAll();
        for (var campus : campuses) {
            //log.info("Обновление участников для кампуса: {}", campus.getId());
            getCampusParticipants(campus.getId());
        }
        log.info("Участники обновлены");
        return CompletableFuture.completedFuture(null);
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> getAllCoalitions() throws ApiException {
        var campuses = campusRepository.findAll();
        for (var campus : campuses) {
            //log.info("Обновление коалиций для кампуса: {}", campus.getId());
            getCampusCoalitions(campus.getId());
        }
        log.info("Коалиции обновлены");
        return CompletableFuture.completedFuture(null);
    }

    public void getCampusCoalitions(String campusId) throws ApiException {
        log.info("Обновление коалиций для кампуса: {}", campusId);

        int offset = 0;
        final int limit = 1000;

        UUID campusIdStr = UUID.fromString(campusId);
        log.info("Получение списка коалиций для кампуса {}", campusIdStr);
        var response = campusApi.getCoalitionsByCampus(campusIdStr, limit, offset);
        if (response != null && !response.getCoalitions().isEmpty()) {
            List<CoalitionV1DTO> coalitions = response.getCoalitions();
            log.info("Получено {} коалиций для кампуса {} на странице с offset {}", coalitions.size(), campusIdStr, offset);
            // Для каждой полученной коалиции маппим в сущность и сохраняем
            ArrayList<Coalition> coalitionEntities = new ArrayList<>();
            for (CoalitionV1DTO coalition : coalitions) {
                log.info("Добавляем коалицию {} в кампус {}", coalition.getCoalitionId(), campusIdStr);
                Coalition coalitionEntity = new Coalition();
                coalitionEntity.setCoalitionId(coalition.getCoalitionId());
                coalitionEntity.setName(coalition.getName());
                coalitionEntity.setCampusId(campusId);
                coalitionEntities.add(coalitionEntity);
            }
            log.info("Сохраняем {} коалиций для кампуса {}", coalitionEntities.size(), campusIdStr);
            coalitionRepository.saveAll(coalitionEntities);
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
                ArrayList<Participant> participantEntities = new ArrayList<>();
                for (String login : logins) {
                    //log.info("Добавляем участника {} в кампус {}", login, campusIdStr);
                    Participant participant = new Participant();
                    participant.setLogin(login);
                    participant.setCampusId(campusId);
                    participantEntities.add(participant);
                }
                log.info("Сохраняем {} участников для кампуса {}", participantEntities.size(), campusIdStr);
                participantRepository.saveAll(participantEntities);

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
