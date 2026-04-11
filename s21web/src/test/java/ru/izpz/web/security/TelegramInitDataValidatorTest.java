package ru.izpz.web.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramInitDataValidatorTest {

    private static final String BOT_TOKEN = "123456:TEST_BOT_TOKEN";

    @Test
    void isValid_returnsTrueForProperlySignedData() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        String initData = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42,\"first_name\":\"Mike\"}"
        ));

        assertTrue(validator.isValid(initData));
    }

    @Test
    void isValid_returnsFalseWhenSignatureIsTampered() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        String initData = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42}"
        ));

        assertFalse(validator.isValid(initData + "00"));
    }

    @Test
    void isValid_returnsFalseWhenAuthDateIsExpired() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofMinutes(1));
        String initData = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().minusSeconds(120).getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42}"
        ));

        assertFalse(validator.isValid(initData));
    }

    private String sign(Map<String, String> params) {
        String dataCheckString = params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("\n"));

        byte[] secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8), BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
        String hash = toHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        return params.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&")) + "&hash=" + hash;
    }

    private byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
