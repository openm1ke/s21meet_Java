package ru.izpz.edu.client;

import java.time.OffsetDateTime;
import java.util.UUID;
import ru.izpz.dto.model.ClusterMapV1DTO;
import ru.izpz.dto.model.ClustersV1DTO;
import ru.izpz.dto.model.EventsV1DTO;
import ru.izpz.dto.model.ParticipantCoalitionV1DTO;
import ru.izpz.dto.model.ParticipantLoginsV1DTO;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.dto.model.ParticipantV1DTO;

/**
 * Фасад REST-доступа к внешней платформе.
 */
public interface PlatformApiFacade {

  /**
   * Возвращает кластеры кампуса.
   *
   * @param campusId идентификатор кампуса
   * @return данные кластеров
   */
  ClustersV1DTO getClustersByCampus(UUID campusId);

  /**
   * Возвращает участников кластера.
   *
   * @param clusterId идентификатор кластера
   * @param limit размер страницы
   * @param offset смещение
   * @param occupied фильтр занятых мест
   * @return данные по местам кластера
   */
  ClusterMapV1DTO getParticipantsByClusterId(
      Long clusterId, Integer limit, Integer offset, Boolean occupied);

  /**
   * Возвращает логины участников кампуса.
   *
   * @param campusId идентификатор кампуса
   * @param limit размер страницы
   * @param offset смещение
   * @return список логинов
   */
  ParticipantLoginsV1DTO getParticipantsByCampusId(UUID campusId, long limit, long offset);

  /**
   * Возвращает события за интервал.
   *
   * @param from начало интервала
   * @param to конец интервала
   * @param type тип события
   * @param limit размер страницы
   * @param offset смещение
   * @return список событий
   */
  EventsV1DTO getEvents(
      OffsetDateTime from, OffsetDateTime to, String type, Long limit, Long offset);

  /**
   * Возвращает проекты участника.
   *
   * @param login логин участника
   * @param limit размер страницы
   * @param offset смещение
   * @param status фильтр статуса
   * @return список проектов
   */
  ParticipantProjectsV1DTO getParticipantProjectsByLogin(
      String login, long limit, long offset, String status);

  /**
   * Возвращает коалицию участника.
   *
   * @param login логин участника
   * @return коалиция участника
   */
  ParticipantCoalitionV1DTO getCoalitionByLogin(String login);

  /**
   * Возвращает участников коалиции.
   *
   * @param coalitionId идентификатор коалиции
   * @param limit размер страницы
   * @param offset смещение
   * @return логины участников
   */
  ParticipantLoginsV1DTO getParticipantsByCoalitionId(Long coalitionId, int limit, int offset);

  /**
   * Возвращает участника по логину.
   *
   * @param login логин участника
   * @return участник
   */
  ParticipantV1DTO getParticipantByLogin(String login);
}
