package ru.izpz.bot.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import ru.izpz.bot.service.MetricsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotMetricsAspectTest {

    @Mock
    private MetricsService metricsService;

    private BotMetricsAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new BotMetricsAspect(metricsService);
    }

    @Test
    void trackTelegramExecute_success_recordsSuccessOutcome() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        BotApiMethod<?> method = mock(BotApiMethod.class);
        when(method.getMethod()).thenReturn("sendMessage");
        when(joinPoint.getArgs()).thenReturn(new Object[]{method});
        Optional<String> response = Optional.of("ok");
        when(joinPoint.proceed()).thenReturn(response);

        Object result = aspect.trackTelegramExecute(joinPoint);

        assertSame(response, result);
        verify(metricsService).recordTelegramApiRequest("sendMessage", "success");
    }

    @Test
    void trackTelegramExecute_emptyResponse_recordsErrorOutcome() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        BotApiMethod<?> method = mock(BotApiMethod.class);
        when(method.getMethod()).thenReturn("sendMessage");
        when(joinPoint.getArgs()).thenReturn(new Object[]{method});
        when(joinPoint.proceed()).thenReturn(Optional.empty());

        Object result = aspect.trackTelegramExecute(joinPoint);

        assertSame(Optional.empty(), result);
        verify(metricsService).recordTelegramApiRequest("sendMessage", "error");
    }

    @Test
    void trackTelegramExecute_exception_recordsErrorAndRethrows() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> aspect.trackTelegramExecute(joinPoint));

        verify(metricsService).recordTelegramApiRequest("unknown", "error");
    }
}
