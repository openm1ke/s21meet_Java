package ru.izpz.edu.scheduler.metrics;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.service.SchedulerMetricsService;

@Aspect
@Component
@RequiredArgsConstructor
public class SchedulerMetricsAspect {

    private final SchedulerMetricsService schedulerMetricsService;

    @Around("@annotation(trackSchedulerMetrics)")
    public Object recordSchedulerMetrics(ProceedingJoinPoint pjp, TrackSchedulerMetrics trackSchedulerMetrics) throws Throwable {
        String scheduler = trackSchedulerMetrics.scheduler();
        String phase = trackSchedulerMetrics.phase();
        Timer.Sample sample = schedulerMetricsService.startPhaseTimer();

        try {
            Object result = pjp.proceed();
            schedulerMetricsService.recordPhaseRequest(scheduler, phase, "success");
            schedulerMetricsService.recordRunStatus(scheduler, "success");
            schedulerMetricsService.recordLastSuccess(scheduler);
            return result;
        } catch (Throwable throwable) {
            schedulerMetricsService.recordPhaseRequest(scheduler, phase, "error");
            schedulerMetricsService.recordPhaseIssue(scheduler, phase, classifyPhaseIssue(throwable));
            schedulerMetricsService.recordRunStatus(scheduler, "failed");
            throw throwable;
        } finally {
            schedulerMetricsService.stopPhaseTimer(scheduler, phase, sample);
        }
    }

    private String classifyPhaseIssue(Throwable throwable) {
        if (throwable instanceof ApiException) {
            return "api_exception";
        }
        String className = throwable.getClass().getSimpleName().toLowerCase();
        if (className.contains("timeout")) {
            return "timeout";
        }
        if (className.contains("interrupted")) {
            return "interrupted";
        }
        return "execution_exception";
    }
}
