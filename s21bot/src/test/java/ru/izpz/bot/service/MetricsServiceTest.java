package ru.izpz.bot.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void testRecordButtonPress() {
        // Given
        Long userId = 12345L;
        String buttonCode = "SEARCH";
        String buttonType = "keyboard";
        
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        // When
        metricsService.recordButtonPress(userId, buttonCode, buttonType);

        // Then
        verify(meterRegistry).counter(
            eq("bot_button_press_total"),
            eq("user_id"), eq("12345"),
            eq("button"), eq("SEARCH"),
            eq("type"), eq("keyboard")
        );
        verify(counter).increment();
    }

    @Test
    void testRecordButtonPressWithInlineButton() {
        // Given
        Long userId = 67890L;
        String buttonCode = "add_friend";
        String buttonType = "inline";
        
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        // When
        metricsService.recordButtonPress(userId, buttonCode, buttonType);

        // Then
        verify(meterRegistry).counter(
            eq("bot_button_press_total"),
            eq("user_id"), eq("67890"),
            eq("button"), eq("add_friend"),
            eq("type"), eq("inline")
        );
        verify(counter).increment();
    }
}
