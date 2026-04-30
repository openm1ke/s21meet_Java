package ru.izpz.edu.client;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.service.GraphQLService;

/**
 * Клиент доступа к данным кампусов и проектам участников.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusClient {

  private final PlatformApiFacade platformApi;
  private final GraphQLService graphQlService;

  /**
   * Возвращает список кластеров кампуса.
   *
   * @param campusId идентификатор кампуса
   * @return список кластеров
   */
  public List<ClusterV1DTO> getClustersByCampus(String campusId) {
    var response = platformApi.getClustersByCampus(UUID.fromString(campusId));

    if (response == null) {
      log.warn("API вернул null для кампуса {}", campusId);
      throw new PlatformClientException("API вернул null для кампуса " + campusId, null);
    }
    return response.getClusters();
  }

  /**
   * Возвращает занятые места в кластере.
   *
   * @param clusterId идентификатор кластера
   * @return список занятых мест
   */
  public List<WorkplaceV1DTO> getParticipantsByCluster(Long clusterId) {
    // получение занятых мест в кластере (самый большой кластер 138 мест, поэтому выставляем
    // максимум)
    var response = platformApi.getParticipantsByClusterId(clusterId, 1000, 0, true);

    if (response == null) {
      log.warn("API вернул null для кластера {}", clusterId);
      throw new PlatformClientException("API вернул null для кластера " + clusterId, null);
    }
    return response.getClusterMap();
  }

  /**
   * Возвращает занятые места в кластере через GraphQL.
   *
   * @param clusterId идентификатор кластера
   * @return список мест
   */
  public List<GraphQLService.ClusterSeat> getParticipantsByClusterV2(Long clusterId) {
    return graphQlService.getOccupiedSeats(String.valueOf(clusterId));
  }

  /**
   * Возвращает проекты участника по логину.
   *
   * @param login логин участника
   * @return список проектов
   */
  public List<StudentProjectData> getStudentProjectsByLogin(String login) {
    return graphQlService.getStudentProjectsByLogin(login);
  }

  /**
   * Возвращает логины участников кампуса постранично.
   *
   * @param campusId идентификатор кампуса
   * @param limit размер страницы
   * @param offset смещение
   * @return список логинов
   */
  public List<String> getParticipantsByCampus(String campusId, long limit, long offset) {
    var response = platformApi.getParticipantsByCampusId(UUID.fromString(campusId), limit, offset);
    if (response == null) {
      log.warn("API вернул null для списка участников кампуса {}", campusId);
      throw new PlatformClientException(
          "API вернул null для списка участников кампуса " + campusId, null);
    }
    return response.getParticipants();
  }
}
