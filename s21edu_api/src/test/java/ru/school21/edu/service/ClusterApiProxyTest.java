package ru.school21.edu.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import ru.school21.edu.ApiException;
import ru.school21.edu.BaseTestContainer;
import ru.school21.edu.exception.NonRetryableApiException;
import ru.school21.edu.exception.RetryableApiException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@TestPropertySource(properties = {"cluster.api.enabled=true", "api.client.enabled=true", "token.service.enabled=true"})
class ClusterApiProxyTest extends BaseTestContainer {

    @SpyBean
    private ClusterApiProxy clusterApiProxy;

    @MockBean
    private TokenService tokenService;

    @Test
    void getParticipantsByCoalitionId1() throws ApiException {
        // Задаём тестовые параметры
        Long clusterId = 123L;
        Integer limit = 100;
        Integer offset = 0;
        Boolean occupied = false;

        when(tokenService.getToken()).thenReturn("token");
        // Подменяем поведение: метод всегда выбрасывает ApiException с кодом 429.
        doThrow(new ApiException(429, "Too many requests"))
                .when(clusterApiProxy)
                .getParticipantsByCoalitionId1(clusterId, limit, offset, occupied);

        // Проверяем, что после повторных попыток выбрасывается RetryableApiException.
        // Благодаря аспекту RetryableApiException должен быть выброшен.
        RetryableApiException ex = assertThrows(RetryableApiException.class, () -> {
            clusterApiProxy.getParticipantsByCoalitionId1(clusterId, limit, offset, occupied);
        });

        assertThat(ex.getMessage()).contains("Too many requests");

        // Проверяем, что метод вызывался хотя бы несколько раз, что говорит о повторных попытках.
        verify(clusterApiProxy, atLeast(3))
                .getParticipantsByCoalitionId1(clusterId, limit, offset, occupied);
    }

    @Test
    void getParticipantsByCoalitionId2() throws ApiException {
        Long clusterId = 123L;
        Integer limit = 100;
        Integer offset = 0;
        Boolean occupied = false;

        when(tokenService.getToken()).thenReturn("token");
        // Подменяем поведение: метод всегда выбрасывает ApiException с кодом 429.
        doThrow(new ApiException(400, "Bad request"))
                .when(clusterApiProxy)
                .getParticipantsByCoalitionId1(clusterId, limit, offset, occupied);

        NonRetryableApiException ex = assertThrows(NonRetryableApiException.class, () -> {
            clusterApiProxy.getParticipantsByCoalitionId1(clusterId, limit, offset, occupied);
        });

        assertThat(ex.getMessage()).contains("Bad request");

    }
}