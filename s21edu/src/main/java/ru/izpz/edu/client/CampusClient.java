package ru.izpz.edu.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.service.GraphQLService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampusClient {

    private final CampusApi campusApi;
    private final ClusterApi clusterApi;
    private final GraphQLService graphQLService;

    /**
     * Получение списка кластеров для кампуса
     * @param campusId айди кампуса
     * @throws ApiException исключение
     */
    public List<ClusterV1DTO> getClustersByCampus(String campusId) throws ApiException {
        //log.info("Получение списка кластеров для кампуса {}", campusId);
        var response = campusApi.getClustersByCampus(UUID.fromString(campusId));

        if (response == null) {
            log.warn("API вернул null для кампуса {}", campusId);
            throw new ApiException("API вернул null для кампуса " + campusId);
        }
        //log.info("Получено {} кластеров для кампуса {}", response.getClusters().size(), campusId);
        return response.getClusters();
    }

    /**
     * Метод получения списка занятых рабочих мест по кластерам
     * @param clusterId айди кластера определенного кампуса
     * @throws ApiException исключение
     */
    public List<WorkplaceV1DTO> getParticipantsByCluster(Long clusterId) throws ApiException {
        //log.info("Получение списка занятых рабочих мест по кластерам {}", clusterId);
        // получение занятых мест в кластере (самый большой кластер 138 мест, поэтому выставляем максимум)
        var response = clusterApi.getParticipantsByCoalitionId1(clusterId, 1000, 0, true);

        if (response == null) {
           log.warn("API вернул null для кластера {}", clusterId);
           throw new ApiException("API вернул null для кластера " + clusterId);
        }
        //log.info("Получено {} участников для кластера {} на странице", response.getClusterMap().size(), clusterId);
        return response.getClusterMap();
    }


    public void getParticipantsByClusterV2(Long clusterId) throws ApiException {
        log.info("Получение списка занятых рабочих мест по кластерам {} через GraphQL", clusterId);
        var response = graphQLService.getOccupiedSeats(String.valueOf(clusterId));
        for (var workplace : response) {
            System.out.println(workplace);
        }
    }
}
