package ru.izpz.bot.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import ru.izpz.bot.service.MetricsService;

import java.util.Optional;

@Aspect
@Component
public class BotMetricsAspect {

    private final MetricsService metricsService;

    public BotMetricsAspect(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Around("execution(public java.util.Optional ru.izpz.bot.service.TelegramExecutorService.execute(..))")
    public Object trackTelegramExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        String telegramMethod = extractTelegramMethod(joinPoint.getArgs());
        String outcome = "error";
        try {
            Object result = joinPoint.proceed();
            boolean success = result instanceof Optional<?> optional && optional.isPresent();
            outcome = success ? "success" : "error";
            return result;
        } finally {
            metricsService.recordTelegramApiRequest(telegramMethod, outcome);
        }
    }

    private String extractTelegramMethod(Object[] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof BotApiMethod<?> method)) {
            return "unknown";
        }
        String methodName = method.getMethod();
        return methodName == null || methodName.isBlank() ? "unknown" : methodName;
    }
}
