package ru.izpz.bot.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void recordButtonPress(Long userId, String buttonCode, String buttonType) {
        log.info("🔥 Button pressed: {} by user {} (type: {})", buttonCode, userId, buttonType);
        
        Counter.builder("bot_button_press_total")
                .description("Total number of button presses by users")
                .tag("user_id", userId.toString())
                .tag("button", buttonCode)
                .tag("type", buttonType)
                .register(meterRegistry)
                .increment();
    }
}
