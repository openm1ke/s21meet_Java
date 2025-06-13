package ru.izpz.rocket.client;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.izpz.dto.RocketChatSendResponse;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RocketChatWebSocketClient extends WebSocketClient {

    private final String token;
    private final String targetUsername;
    private final String messageToSend;
    private final boolean isQrMode;

    private final CountDownLatch latch = new CountDownLatch(1);

    private volatile RocketChatSendResponse response;

    private static final String ENTER_COMMAND = "/enter";
    private static final String EXPECTED_QR_MSG = "The QR code will expire on";

    public RocketChatWebSocketClient(String webSocketUri, String token, String targetUsername, String messageToSend, boolean isQrMode) {
        super(URI.create(webSocketUri));
        this.token = token;
        this.targetUsername = targetUsername;
        this.messageToSend = messageToSend;
        this.isQrMode = isQrMode;
    }

    public RocketChatSendResponse execute(long timeoutSeconds) {
        try {
            connectBlocking();
            boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                return new RocketChatSendResponse(false, "Timeout waiting for Rocket.Chat response");
            }
            return response != null ? response : new RocketChatSendResponse(false, "Unexpected empty response");
        } catch (Exception e) {
            log.error("WebSocket execution error", e);
            return new RocketChatSendResponse(false, "Error: " + e.getMessage());
        } finally {
            close();
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("Connected to Rocket.Chat");
        sendConnectMessage();
        sendLoginMessage();
    }

    @Override
    public void onMessage(String msg) {
        log.debug("Received message: {}", msg);
        JSONObject json = new JSONObject(msg);

        switch (json.optString("msg")) {
            case "ping" -> sendPong();
            case "result" -> handleResult(json);
            case "changed" -> handleChanged(json);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("WebSocket closed: {} - {}", code, reason);
        latch.countDown();
    }

    @Override
    public void onError(Exception e) {
        log.error("WebSocket error", e);
        response = new RocketChatSendResponse(false, "WebSocket error: " + e.getMessage());
        latch.countDown();
    }

    private void sendConnectMessage() {
        send(new JSONObject()
                .put("msg", "connect")
                .put("version", "1")
                .put("support", new String[]{"1", "pre2", "pre1"})
                .toString());
    }

    private void sendLoginMessage() {
        send(new JSONObject()
                .put("msg", "method")
                .put("method", "login")
                .put("id", "42")
                .put("params", new JSONArray().put(new JSONObject().put("resume", token)))
                .toString());
    }

    private void handleResult(JSONObject json) {
        String id = json.optString("id");

        if ("42".equals(id)) {
            if (json.has("error")) {
                response = new RocketChatSendResponse(false, "Login failed: " + json.getJSONObject("error").optString("message"));
                close();
            } else {
                openDirectMessage(targetUsername);
            }
        } else if ("unique_create_dm_id".equals(id)) {
            if (json.has("error")) {
                response = new RocketChatSendResponse(false, "Failed to create DM: " + json.getJSONObject("error").optString("message"));
                close();
            } else {
                String roomId = json.getJSONObject("result").optString("rid");
                if (isQrMode) {
                    subscribeToRoom(roomId);
                    sendSlashCommand(roomId, ENTER_COMMAND);
                } else {
                    sendTextMessage(roomId, messageToSend);
                }
            }
        }
    }

    private void handleChanged(JSONObject json) {
        JSONObject args = json.optJSONObject("fields")
                .optJSONArray("args")
                .optJSONObject(0);

        if (args != null) {
            String received = args.optString("msg");
            if (received.contains(EXPECTED_QR_MSG)) {
                response = new RocketChatSendResponse(true, received);
                close();
            }
        }
    }

    private void sendPong() {
        send(new JSONObject().put("msg", "pong").toString());
    }

    private void openDirectMessage(String username) {
        send(new JSONObject()
                .put("msg", "method")
                .put("method", "createDirectMessage")
                .put("id", "unique_create_dm_id")
                .put("params", new JSONArray().put(username))
                .toString());
    }

    private void subscribeToRoom(String roomId) {
        send(new JSONObject()
                .put("msg", "sub")
                .put("id", "unique_subscription_id")
                .put("name", "stream-room-messages")
                .put("params", new JSONArray().put(roomId).put(true))
                .toString());
    }

    private void sendSlashCommand(String roomId, String command) {
        send(new JSONObject()
                .put("msg", "method")
                .put("method", "slashCommand")
                .put("id", "unique_command_id")
                .put("params", new JSONArray().put(new JSONObject()
                        .put("cmd", command.substring(1))
                        .put("params", "")
                        .put("msg", new JSONObject().put("rid", roomId))))
                .toString());
    }

    private void sendTextMessage(String roomId, String text) {
        send(new JSONObject()
                .put("msg", "method")
                .put("method", "sendMessage")
                .put("id", "unique_send_message_id")
                .put("params", new JSONArray().put(new JSONObject()
                        .put("rid", roomId)
                        .put("msg", text)))
                .toString());

        response = new RocketChatSendResponse(true, text);
        close();
    }
}
