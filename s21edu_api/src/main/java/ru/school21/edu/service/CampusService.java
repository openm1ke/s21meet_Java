package ru.school21.edu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.school21.edu.ApiClient;
import ru.school21.edu.ApiException;
import ru.school21.edu.model.Cluster;
import ru.school21.edu.model.Workplace;
import ru.school21.edu.model.WorkplaceId;
import ru.school21.edu.repository.ClusterRepository;
import ru.school21.edu.repository.WorkplaceRepository;
import ru.school21.edu.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CampusService {

    private final CampusApiProxy campusApi;
    private final ClusterApiProxy clusterApi;
    private final ClusterRepository clusterRepository;
    private final WorkplaceRepository workplaceRepository;
    private final ApiClient apiClient;
    private final TokenService tokenService;

    public CampusService(CampusApiProxy campusApi,
                         ClusterApiProxy clusterApi,
                         ClusterRepository clusterRepository,
                         WorkplaceRepository workplaceRepository,
                         ApiClient apiClient,
                         TokenService tokenService) {
        this.campusApi = campusApi;
        this.clusterApi = clusterApi;
        this.clusterRepository = clusterRepository;
        this.workplaceRepository = workplaceRepository;
        this.apiClient = apiClient;
        this.tokenService = tokenService;
    }

    @Scheduled(fixedDelay = 30000)
    public void parseMskKznNsk() {
        // установка токена для всех клиентов
        apiClient.setApiKey(tokenService.getToken());
        // список целевых кампусов
        List<String> campuses = List.of(
            "6bfe3c56-0211-4fe1-9e59-51616caac4dd", // MSK
            "7c293c9c-f28c-4b10-be29-560e4b000a34", // KZN
            "46e7d965-21e9-4936-bea9-f5ea0d1fddf2"  // NSK
        );

        log.info("Получение кластеров для Москвы, Казани и Новосибирска");
        campuses.parallelStream().forEach(campus -> {
            try {
                getClustersByCampus(UUID.fromString(campus));
            } catch (ApiException e) {
                log.info("Ошибка получения кластеров для кампуса {}", campus);
            }
        });

        List<Cluster> clusters = clusterRepository.findAll();

        clusters.parallelStream().forEach(cluster -> {
            try {
                getParticipantsByCluster(cluster.getClusterId());
            } catch (ApiException e) {
                log.info("Ошибка получения участников для кластера {}", cluster.getClusterId());
            }
        });

        log.info("Данные участников из Москвы, Казани и Новосибирска по кластерам обновлены.");
    }

    /**
     * Метод получения списка занятых рабочих мест по кластерам
     * @param clusterId айди кластера определенного кампуса
     * @throws ApiException исключение
     */
    public void getParticipantsByCluster(Long clusterId) throws ApiException {

        //log.info("Получение списка занятых рабочих мест по кластерам {}", clusterId);
        int offset = 0;
        final int limit = 1000;

        var response = clusterApi.getParticipantsByCoalitionId1(clusterId, limit, offset, true);
        if(response != null && !response.getClusterMap().isEmpty()) {
            var clusterMap = response.getClusterMap();
            //log.info("Получено {} участников для кластера {} на странице с offset {}", clusterMap.size(), clusterId, offset);
            // Для каждого полученного логина маппим в сущность и сохраняем
            ArrayList<Workplace> workplaces = new ArrayList<>();
            for (var workplace : clusterMap) {
                //log.info("Добавляем участника {} из кластера {}", workplace.getLogin(), clusterId);
                WorkplaceId workplaceId = new WorkplaceId(clusterId, workplace.getRow(), workplace.getNumber());
                Workplace workplaceEntity = new Workplace();
                workplaceEntity.setId(workplaceId);
                workplaceEntity.setLogin(StringUtils.extractLogin(workplace.getLogin()));
                workplaces.add(workplaceEntity);
            }
            //log.info("Сохраняем {} участников для кластера {}", workplaces.size(), clusterId);
            workplaceRepository.deleteByIdClusterId(clusterId);
            workplaceRepository.saveAllAndFlush(workplaces);
        }
    }

    /**
     * Получение списка кластеров для кампуса
     * @param campusId айди кампуса
     * @throws ApiException исключение
     */
    public void getClustersByCampus(UUID campusId) throws ApiException {
        //log.info("Получение списка кластеров для кампуса {}", campusId);
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
        //log.info("Сохраняем {} кластеров для кампуса {}", clusterEntities.size(), campusId);
        clusterRepository.saveAllAndFlush(clusterEntities);
    }
}
