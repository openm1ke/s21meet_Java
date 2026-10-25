package ru.izpz.bot.service;

import java.time.OffsetDateTime;
import ru.izpz.dto.RocketChatSendResponse;

public record VerificationCodeDelivery(RocketChatSendResponse response, OffsetDateTime expiresAt) {}
