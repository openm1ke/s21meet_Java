package ru.school21.edu.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import ru.school21.edu.ApiException;
import ru.school21.edu.exception.NonRetryableApiException;
import ru.school21.edu.exception.RetryableApiException;

@Slf4j
@Aspect
@Component
public class ApiExceptionTranslatorAspect {

    @Around("execution(* ru.school21.edu.service..*(..))")
    public Object translateApiException(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (ApiException e) {
            int code = e.getCode();
            if (code == 429 || code == 500) {
                log.warn("Retryable error in {}: {}", joinPoint.getSignature(), e.getCode());
                throw new RetryableApiException(code, e.getMessage(), e);
            } else {
                log.warn("Non-retryable error in {}: {}", joinPoint.getSignature(), e.getCode());
                throw new NonRetryableApiException(code, e.getMessage(), e);
            }
        }
    }
}