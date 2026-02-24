package ru.izpz.bot.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void testRecordButtonPress() {
        // Given
        Long userId = 12345L;
        String buttonCode = "SEARCH";
        String buttonType = "keyboard";

        // When
        metricsService.recordButtonPress(userId, buttonCode, buttonType);

        // Then
        double count = meterRegistry.get("bot_button_press_total")
                .tag("user_id", "12345")
                .tag("button", "SEARCH")
                .tag("type", "keyboard")
                .counter()
                .count();
        assertEquals(1.0, count, 0.001);
    }

    @Test
    void testRecordButtonPressWithInlineButton() {
        // Given
        Long userId = 67890L;
        String buttonCode = "add_friend";
        String buttonType = "inline";

        // When
        metricsService.recordButtonPress(userId, buttonCode, buttonType);

        // Then
        double count = meterRegistry.get("bot_button_press_total")
                .tag("user_id", "67890")
                .tag("button", "add_friend")
                .tag("type", "inline")
                .counter()
                .count();
        assertEquals(1.0, count, 0.001);
    }
}
