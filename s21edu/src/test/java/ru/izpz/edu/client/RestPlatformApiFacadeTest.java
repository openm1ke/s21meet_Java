package ru.izpz.edu.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.api.CoalitionApi;
import ru.izpz.dto.api.EventApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ClusterMapV1DTO;
import ru.izpz.dto.model.ClustersV1DTO;
import ru.izpz.dto.model.EventsV1DTO;
import ru.izpz.dto.model.ParticipantCoalitionV1DTO;
import ru.izpz.dto.model.ParticipantLoginsV1DTO;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.exception.PlatformClientException;

class RestPlatformApiFacadeTest {

  private CampusApi campusApi;
  private ClusterApi clusterApi;
  private ParticipantApi participantApi;
  private CoalitionApi coalitionApi;
  private EventApi eventApi;

  private RestPlatformApiFacade facade;

  @BeforeEach
  void setUp() {
    campusApi = mock(CampusApi.class);
    clusterApi = mock(ClusterApi.class);
    participantApi = mock(ParticipantApi.class);
    coalitionApi = mock(CoalitionApi.class);
    eventApi = mock(EventApi.class);
    facade =
        new RestPlatformApiFacade(campusApi, clusterApi, participantApi, coalitionApi, eventApi);
  }

  @Test
  void getClustersByCampus_shouldReturnResponse() throws Exception {
    UUID campusId = UUID.randomUUID();
    ClustersV1DTO expected = new ClustersV1DTO().clusters(List.of());
    when(campusApi.getClustersByCampus(campusId)).thenReturn(expected);

    ClustersV1DTO actual = facade.getClustersByCampus(campusId);

    assertSame(expected, actual);
  }

  @Test
  void getParticipantsByClusterId_shouldReturnResponse() throws Exception {
    ClusterMapV1DTO expected = new ClusterMapV1DTO();
    when(clusterApi.getParticipantsByCoalitionId1(10L, 20, 0, true)).thenReturn(expected);

    ClusterMapV1DTO actual = facade.getParticipantsByClusterId(10L, 20, 0, true);

    assertSame(expected, actual);
  }

  @Test
  void getParticipantsByCampusId_shouldReturnResponse() throws Exception {
    UUID campusId = UUID.randomUUID();
    ParticipantLoginsV1DTO expected = new ParticipantLoginsV1DTO().participants(List.of());
    when(campusApi.getParticipantsByCampusId(campusId, 50L, 0L)).thenReturn(expected);

    ParticipantLoginsV1DTO actual = facade.getParticipantsByCampusId(campusId, 50L, 0L);

    assertSame(expected, actual);
  }

  @Test
  void getEvents_shouldReturnResponse() throws Exception {
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = from.plusDays(1);
    EventsV1DTO expected = new EventsV1DTO().events(List.of());
    when(eventApi.getEvents(from, to, "meeting", 100L, 0L)).thenReturn(expected);

    EventsV1DTO actual = facade.getEvents(from, to, "meeting", 100L, 0L);

    assertSame(expected, actual);
  }

  @Test
  void getParticipantProjectsByLogin_shouldReturnResponse() throws Exception {
    ParticipantProjectsV1DTO expected = new ParticipantProjectsV1DTO().projects(List.of());
    when(participantApi.getParticipantProjectsByLogin("quarkron", 10L, 0L, "IN_PROGRESS"))
        .thenReturn(expected);

    ParticipantProjectsV1DTO actual =
        facade.getParticipantProjectsByLogin("quarkron", 10L, 0L, "IN_PROGRESS");

    assertSame(expected, actual);
  }

  @Test
  void getCoalitionByLogin_shouldReturnResponse() throws Exception {
    ParticipantCoalitionV1DTO expected = new ParticipantCoalitionV1DTO();
    when(participantApi.getCoalitionByLogin("quarkron")).thenReturn(expected);

    ParticipantCoalitionV1DTO actual = facade.getCoalitionByLogin("quarkron");

    assertSame(expected, actual);
  }

  @Test
  void getParticipantsByCoalitionId_shouldReturnResponse() throws Exception {
    ParticipantLoginsV1DTO expected = new ParticipantLoginsV1DTO().participants(List.of());
    when(coalitionApi.getParticipantsByCoalitionId(7L, 100, 0)).thenReturn(expected);

    ParticipantLoginsV1DTO actual = facade.getParticipantsByCoalitionId(7L, 100, 0);

    assertSame(expected, actual);
  }

  @Test
  void getParticipantByLogin_shouldReturnResponse() throws Exception {
    ParticipantV1DTO expected = new ParticipantV1DTO();
    when(participantApi.getParticipantByLogin("quarkron")).thenReturn(expected);

    ParticipantV1DTO actual = facade.getParticipantByLogin("quarkron");

    assertSame(expected, actual);
  }

  @Test
  void getClustersByCampus_shouldMapRestClientResponseExceptionToPlatformException()
      throws Exception {
    UUID campusId = UUID.randomUUID();
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Test", "ok");
    RestClientResponseException ex =
        new RestClientResponseException(
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            headers,
            "bad-body".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);
    when(campusApi.getClustersByCampus(campusId)).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(PlatformClientException.class, () -> facade.getClustersByCampus(campusId));
    assertEquals(400, apiException.getCode());
    assertEquals("bad-body", apiException.getResponseBody());
    assertEquals(List.of("ok"), apiException.getResponseHeaders().get("X-Test"));
  }

  @Test
  void getParticipantByLogin_shouldMapRestClientResponseExceptionWithNullHeaders()
      throws Exception {
    RestClientResponseException ex =
        new RestClientResponseException(
            "Not Found",
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            null,
            "missing".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);
    when(participantApi.getParticipantByLogin("ghost")).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(PlatformClientException.class, () -> facade.getParticipantByLogin("ghost"));
    assertEquals(404, apiException.getCode());
    assertTrue(apiException.getResponseHeaders().isEmpty());
  }

  @Test
  void getEvents_shouldWrapRestClientException() throws Exception {
    RestClientException ex = new RestClientException("transport error");
    when(eventApi.getEvents(null, null, null, null, null)).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(
            PlatformClientException.class, () -> facade.getEvents(null, null, null, null, null));
    assertSame(ex, apiException.getCause());
  }

  @Test
  void getParticipantsByClusterId_shouldWrapRestClientException() throws Exception {
    RestClientException ex = new RestClientException("transport error");
    when(clusterApi.getParticipantsByCoalitionId1(1L, 10, 0, false)).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(
            PlatformClientException.class,
            () -> facade.getParticipantsByClusterId(1L, 10, 0, false));
    assertSame(ex, apiException.getCause());
  }

  @Test
  void getParticipantsByCampusId_shouldWrapRestClientException() throws Exception {
    RestClientException ex = new RestClientException("transport error");
    UUID campusId = UUID.randomUUID();
    when(campusApi.getParticipantsByCampusId(campusId, 10L, 0L)).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(
            PlatformClientException.class,
            () -> facade.getParticipantsByCampusId(campusId, 10L, 0L));
    assertSame(ex, apiException.getCause());
  }

  @Test
  void getParticipantProjectsByLogin_shouldWrapRestClientException() throws Exception {
    RestClientException ex = new RestClientException("transport error");
    when(participantApi.getParticipantProjectsByLogin("u", 10L, 0L, null)).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(
            PlatformClientException.class,
            () -> facade.getParticipantProjectsByLogin("u", 10L, 0L, null));
    assertSame(ex, apiException.getCause());
  }

  @Test
  void getCoalitionByLogin_shouldWrapRestClientException() throws Exception {
    RestClientException ex = new RestClientException("transport error");
    when(participantApi.getCoalitionByLogin("u")).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(PlatformClientException.class, () -> facade.getCoalitionByLogin("u"));
    assertSame(ex, apiException.getCause());
  }

  @Test
  void getParticipantsByCoalitionId_shouldWrapRestClientException() throws Exception {
    RestClientException ex = new RestClientException("transport error");
    when(coalitionApi.getParticipantsByCoalitionId(1L, 10, 0)).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(
            PlatformClientException.class, () -> facade.getParticipantsByCoalitionId(1L, 10, 0));
    assertSame(ex, apiException.getCause());
  }

  @Test
  void getParticipantsByClusterId_shouldMapRestClientResponseException() throws Exception {
    RestClientResponseException ex =
        new RestClientResponseException(
            "Conflict",
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            null,
            "conflict".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);
    when(clusterApi.getParticipantsByCoalitionId1(2L, 20, 0, true)).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(
            PlatformClientException.class,
            () -> facade.getParticipantsByClusterId(2L, 20, 0, true));
    assertEquals(409, apiException.getCode());
  }

  @Test
  void getParticipantProjectsByLogin_shouldMapRestClientResponseException() throws Exception {
    RestClientResponseException ex =
        new RestClientResponseException(
            "Unauthorized",
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            null,
            "unauthorized".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);
    when(participantApi.getParticipantProjectsByLogin("u2", 1L, 0L, "REGISTERED")).thenThrow(ex);

    PlatformClientException apiException =
        assertThrows(
            PlatformClientException.class,
            () -> facade.getParticipantProjectsByLogin("u2", 1L, 0L, "REGISTERED"));
    assertEquals(401, apiException.getCode());
  }
}
