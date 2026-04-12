package ru.izpz.web.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TelegramInitDataValidator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_PARAM = "hash";
    private static final String AUTH_DATE_PARAM = "auth_date";
    private static final String USER_PARAM = "user";
    private static final String TELEGRAM_ID_PATTERN = "^\\d{5,13}$";

    private final String botToken;
    private final long maxAgeSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramInitDataValidator(
        @Value("${telegram.webapp.auth.bot-token:${BOT_TOKEN:}}") String botToken,
        @Value("${telegram.webapp.auth.max-age:PT1H}") Duration maxAge
    ) {
        this.botToken = botToken;
        this.maxAgeSeconds = Math.max(1L, maxAge.getSeconds());
    }

    public boolean isValid(String initDataRaw) {
        if (!StringUtils.hasText(initDataRaw) || !StringUtils.hasText(botToken)) {
            return false;
        }

        Map<String, String> params = parseQueryString(initDataRaw);
        String hash = params.remove(HASH_PARAM);
        if (!StringUtils.hasText(hash)) {
            return false;
        }

        if (!isFresh(params.get(AUTH_DATE_PARAM))) {
            return false;
        }

        String dataCheckString = params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("\n"));

        String expectedHash = bytesToHex(hmacSha256(secretKey(), dataCheckString.getBytes(StandardCharsets.UTF_8)));
        return MessageDigest.isEqual(
            expectedHash.getBytes(StandardCharsets.US_ASCII),
            hash.toLowerCase().getBytes(StandardCharsets.US_ASCII)
        );
    }

    public String extractTelegramId(String initDataRaw) {
        if (!StringUtils.hasText(initDataRaw)) {
            return null;
        }
        Map<String, String> params = parseQueryString(initDataRaw);
        String userRaw = params.get(USER_PARAM);
        if (!StringUtils.hasText(userRaw)) {
            return null;
        }
        try {
            JsonNode userNode = objectMapper.readTree(userRaw);
            JsonNode idNode = userNode.get("id");
            if (idNode == null || idNode.isNull()) {
                return null;
            }
            String telegramId = idNode.asText();
            if (!StringUtils.hasText(telegramId) || !telegramId.matches(TELEGRAM_ID_PATTERN)) {
                return null;
            }
            return telegramId;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isFresh(String authDateRaw) {
        if (!StringUtils.hasText(authDateRaw)) {
            return false;
        }
        try {
            long authDate = Long.parseLong(authDateRaw);
            long now = Instant.now().getEpochSecond();
            return now >= authDate && now - authDate <= maxAgeSeconds;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private byte[] secretKey() {
        return hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate Telegram WebApp HMAC", ex);
        }
    }

    private Map<String, String> parseQueryString(String raw) {
        Map<String, String> result = new HashMap<>();
        Arrays.stream(raw.split("&"))
            .filter(StringUtils::hasText)
            .forEach(pair -> {
                int index = pair.indexOf('=');
                String keyPart = index >= 0 ? pair.substring(0, index) : pair;
                String valuePart = index >= 0 ? pair.substring(index + 1) : "";
                String key = URLDecoder.decode(keyPart, StandardCharsets.UTF_8);
                String value = URLDecoder.decode(valuePart, StandardCharsets.UTF_8);
                result.put(key, value);
            });
        return result;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
