package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.Clusters;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.dto.CampusDto;
import ru.izpz.edu.exception.ProfileNotFoundException;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.repository.ProfileRepository;
import ru.izpz.edu.repository.WorkplaceRepository;
import ru.izpz.edu.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "campus.service.enabled", havingValue = "true")
public class CampusService {

    private final CampusApiProxy campusApi;
    private final ClusterApiProxy clusterApi;
    private final GraphQLService graphQLService;
    private final ParticipantApiProxy participantApi;
    private final ClusterRepository clusterRepository;
    private final WorkplaceRepository workplaceRepository;
    private final ApiClient apiClient;
    private final TokenService tokenService;
    private final ProfileRepository profileRepository;

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
                log.error("Ошибка получения кластеров для кампуса {}", campus);
            }
        });

        List<Cluster> clusters = clusterRepository.findAll();

        clusters.parallelStream().forEach(cluster -> {
            try {
                getParticipantsByCluster(cluster.getClusterId());
            } catch (ApiException e) {
                log.error("Ошибка получения участников для кластера {}", cluster.getClusterId());
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
        // получение занятых мест в кластере (самый большой кластер 138 мест, поэтому выставляем максимум)
        var response = clusterApi.getParticipantsByCoalitionId1(clusterId, 1000, 0, true);
        // надо удалять старые записи даже если нам ничего не пришло
        workplaceRepository.deleteByIdClusterId(clusterId);
        if(response != null && !response.getClusterMap().isEmpty()) {
            var clusterMap = response.getClusterMap();
            //log.info("Получено {} участников для кластера {} на странице", clusterMap.size(), clusterId);
            // Для каждого полученного логина маппим в сущность и сохраняем
            ArrayList<Workplace> workplaces = new ArrayList<>(clusterMap.size());
            for (var workplace : clusterMap) {
                WorkplaceId workplaceId = new WorkplaceId(clusterId, workplace.getRow(), workplace.getNumber());
                Workplace workplaceEntity = new Workplace();
                workplaceEntity.setId(workplaceId);
                workplaceEntity.setLogin(StringUtils.extractLogin(workplace.getLogin()));
                workplaces.add(workplaceEntity);
            }
            //log.info("Сохраняем {} участников для кластера {}", workplaces.size(), clusterId);
            workplaceRepository.saveAllAndFlush(workplaces);
        }
    }

    public void getParticipantsByClusterV2(Long clusterId) throws ApiException {
        log.info("Получение списка занятых рабочих мест по кластерам {} через GraphQL", clusterId);
        var response = graphQLService.getOccupiedSeats(String.valueOf(clusterId));
        for (var workplace : response) {
            System.out.println(workplace);
        }
    }

    /**
     * Получение списка кластеров для кампуса
     * @param campusId айди кампуса
     * @throws ApiException исключение
     */
    public void getClustersByCampus(UUID campusId) throws ApiException {
        //log.info("Получение списка кластеров для кампуса {}", campusId);
        var response = campusApi.getClustersByCampus(campusId);

        if (response == null) {
            log.warn("API вернул null для кампуса {}", campusId);
            return; // Выход без ошибки
        }
        var clusters = response.getClusters();
        ArrayList<Cluster> clusterEntities = new ArrayList<>(clusters.size());
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


    public ParticipantV1DTO checkEduLogin(String login) throws ApiException {
        log.info("Получен запрос на проверку логина: login = {}", login);
        return participantApi.getParticipantByLogin(login);
    }

    public CampusDto getCampus(String telegramId) throws ApiException {
        log.info("Получен запрос на получение кампуса для telegramId = {}", telegramId);
        var profile = profileRepository.findByTelegramId(telegramId);
        if (profile.isEmpty()) {
            throw new ProfileNotFoundException("Не найден логин для данного телеграм айди");
        }
        var participant = checkEduLogin(profile.get().getS21login());
        return new CampusDto(participant.getCampus().getShortName(), participant.getCampus().getId().toString());
    }

    public List<Clusters> getClusters(CampusDto campus) {
        return clusterRepository.findAllByCampusIdOrderByFloorAsc(campus.getUuid()).stream()
            .map(cluster -> Clusters.builder()
                .name(cluster.getName())
                .capacity(cluster.getCapacity())
                .availableCapacity(cluster.getAvailableCapacity())
                .floor(cluster.getFloor())
                .build())
            .toList();
    }
}
