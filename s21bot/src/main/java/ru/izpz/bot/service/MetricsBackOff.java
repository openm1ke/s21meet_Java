package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.longpolling.interfaces.BackOff;

@RequiredArgsConstructor
public class MetricsBackOff implements BackOff {

    private final BackOff delegate;
    private final MetricsService metricsService;

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public long nextBackOffMillis() {
        metricsService.recordTelegramApiRequest("getUpdates", "error");
        return delegate.nextBackOffMillis();
    }
}
