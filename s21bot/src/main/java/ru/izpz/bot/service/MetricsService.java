package ru.izpz.bot.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void recordButtonPress(String buttonCode, ButtonMetricType buttonType) {
        meterRegistry.counter(
                "bot_button_press_total",
                "button", normalize(buttonCode),
                "type", normalize(buttonType != null ? buttonType.tagValue() : null)
        ).increment();
    }

    public void recordTelegramApiRequest(String method, String outcome) {
        meterRegistry.counter(
                "bot_telegram_api_requests_total",
                "method", normalize(method),
                "outcome", normalize(outcome)
        ).increment();
    }

    public void recordNotifyDelivery(String outcome) {
        meterRegistry.counter(
                "bot_notify_delivery_total",
                "outcome", normalize(outcome)
        ).increment();
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value;
    }
}
