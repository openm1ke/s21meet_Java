package ru.izpz.bot.service;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.longpolling.interfaces.BackOff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricsBackOffTest {

    @Test
    void nextBackOffMillis_recordsGetUpdatesErrorMetricAndDelegates() {
        BackOff delegate = mock(BackOff.class);
        MetricsService metricsService = mock(MetricsService.class);
        when(delegate.nextBackOffMillis()).thenReturn(321L);
        MetricsBackOff metricsBackOff = new MetricsBackOff(delegate, metricsService);

        long result = metricsBackOff.nextBackOffMillis();

        assertEquals(321L, result);
        verify(metricsService).recordTelegramApiRequest("getUpdates", "error");
        verify(delegate).nextBackOffMillis();
    }

    @Test
    void reset_delegatesWithoutMetrics() {
        BackOff delegate = mock(BackOff.class);
        MetricsService metricsService = mock(MetricsService.class);
        MetricsBackOff metricsBackOff = new MetricsBackOff(delegate, metricsService);

        metricsBackOff.reset();

        verify(delegate).reset();
    }
}
