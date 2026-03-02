package ru.izpz.edu.scheduler.metrics;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import ru.izpz.edu.service.SchedulerMetricsService;

@Aspect
@Component
@RequiredArgsConstructor
public class SchedulerMetricsAspect {

    private final SchedulerMetricsService schedulerMetricsService;
    private final SchedulerErrorClassifier schedulerErrorClassifier;

    @Around("@annotation(trackSchedulerMetrics)")
    public Object recordSchedulerMetrics(ProceedingJoinPoint pjp, TrackSchedulerMetrics trackSchedulerMetrics) throws Throwable {
        String scheduler = trackSchedulerMetrics.scheduler();
        String phase = trackSchedulerMetrics.phase();
        Timer.Sample sample = schedulerMetricsService.startPhaseTimer();

        try {
            Object result = pjp.proceed();
            schedulerMetricsService.recordPhaseRequest(scheduler, phase, SchedulerPhaseRequestStatus.SUCCESS);
            schedulerMetricsService.recordRunStatus(scheduler, SchedulerRunStatus.SUCCESS);
            schedulerMetricsService.recordLastSuccess(scheduler);
            return result;
        } catch (Throwable throwable) {
            schedulerMetricsService.recordPhaseRequest(scheduler, phase, SchedulerPhaseRequestStatus.FAILED);
            schedulerMetricsService.recordPhaseIssue(scheduler, phase, schedulerErrorClassifier.classify(throwable));
            schedulerMetricsService.recordRunStatus(scheduler, SchedulerRunStatus.FAILED);
            throw throwable;
        } finally {
            schedulerMetricsService.stopPhaseTimer(scheduler, phase, sample);
        }
    }
}
