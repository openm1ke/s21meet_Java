package ru.izpz.bot.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import ru.izpz.bot.service.MetricsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotMetricsAspectTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private BotApiMethod<?> method;

    @Test
    void trackTelegramExecute_recordsSuccessWhenOptionalPresent() throws Throwable {
        BotMetricsAspect aspect = new BotMetricsAspect(metricsService);
        Optional<String> expected = Optional.of("ok");

        when(joinPoint.getArgs()).thenReturn(new Object[]{method});
        when(method.getMethod()).thenReturn("sendMessage");
        when(joinPoint.proceed()).thenReturn(expected);

        Object result = aspect.trackTelegramExecute(joinPoint);

        assertEquals(expected, result);
        verify(metricsService).recordTelegramApiRequest("sendMessage", "success");
    }

    @Test
    void trackTelegramExecute_recordsErrorWhenOptionalEmptyAndUnknownMethod() throws Throwable {
        BotMetricsAspect aspect = new BotMetricsAspect(metricsService);

        when(joinPoint.getArgs()).thenReturn(new Object[]{method});
        when(method.getMethod()).thenReturn(" ");
        when(joinPoint.proceed()).thenReturn(Optional.empty());

        Object result = aspect.trackTelegramExecute(joinPoint);

        assertEquals(Optional.empty(), result);
        verify(metricsService).recordTelegramApiRequest("unknown", "error");
    }

    @Test
    void trackTelegramExecute_recordsErrorWhenProceedThrows() throws Throwable {
        BotMetricsAspect aspect = new BotMetricsAspect(metricsService);

        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> aspect.trackTelegramExecute(joinPoint));
        verify(metricsService).recordTelegramApiRequest("unknown", "error");
    }
}
