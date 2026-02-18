package ru.izpz.bot.keyboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.exception.InvalidCallbackPayloadException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackPayloadSerializerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void serialize_success_returnsJson() throws Exception {
        CallbackPayload payload = new CallbackPayload("cmd", Map.of("k", "v"));
        when(objectMapper.writeValueAsString(payload)).thenReturn("{\"command\":\"cmd\"}");

        CallbackPayloadSerializer serializer = new CallbackPayloadSerializer(objectMapper);
        String json = serializer.serialize(payload);

        assertEquals("{\"command\":\"cmd\"}", json);
    }

    @Test
    void serialize_whenObjectMapperThrows_wrapsInInvalidCallbackPayloadException() throws Exception {
        CallbackPayload payload = new CallbackPayload("cmd", null);
        when(objectMapper.writeValueAsString(payload)).thenThrow(new JsonProcessingException("x") {});

        CallbackPayloadSerializer serializer = new CallbackPayloadSerializer(objectMapper);

        assertThrows(InvalidCallbackPayloadException.class, () -> serializer.serialize(payload));
    }

    @Test
    void deserialize_success_returnsPayload() throws Exception {
        CallbackPayload payload = new CallbackPayload("cmd", Map.of("k", "v"));
        when(objectMapper.readValue("{}", CallbackPayload.class)).thenReturn(payload);

        CallbackPayloadSerializer serializer = new CallbackPayloadSerializer(objectMapper);
        CallbackPayload result = serializer.deserialize("{}");

        assertEquals("cmd", result.getCommand());
        assertEquals("v", result.getArgs().get("k"));
    }

    @Test
    void deserialize_whenObjectMapperThrows_wrapsInInvalidCallbackPayloadException() throws Exception {
        when(objectMapper.readValue("bad", CallbackPayload.class)).thenThrow(new JsonProcessingException("x") {});

        CallbackPayloadSerializer serializer = new CallbackPayloadSerializer(objectMapper);

        assertThrows(InvalidCallbackPayloadException.class, () -> serializer.deserialize("bad"));
    }
}
