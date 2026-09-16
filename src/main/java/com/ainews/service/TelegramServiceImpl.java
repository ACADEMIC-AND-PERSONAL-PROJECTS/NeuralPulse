package com.ainews.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramServiceImpl implements TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramServiceImpl.class);

    private static final String TELEGRAM_API = "https://api.telegram.org";
    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final RestClient restClient;
    private final String botToken;
    private final String chatId;
    private final Gson gson = new Gson();

    public TelegramServiceImpl(RestClient restClient,
                               @Value("${app.telegram.chat-id:}") String chatId) {
        this.restClient = restClient;
        this.botToken = System.getenv("TELEGRAM_BOT_TOKEN");
        this.chatId = chatId;
    }

    @Override
    public boolean sendMessage(String message) {
        if (botToken == null || botToken.isBlank()) {
            log.error("TELEGRAM_BOT_TOKEN not set");
            return false;
        }
        if (chatId == null || chatId.isBlank()) {
            log.error("TELEGRAM_CHAT_ID not set");
            return false;
        }

        String[] chunks = splitMessage(message);
        boolean allSent = true;

        for (String chunk : chunks) {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("chat_id", chatId);
                body.addProperty("text", chunk);
                body.addProperty("parse_mode", "Markdown");

                String url = TELEGRAM_API + "/bot" + botToken + "/sendMessage";

                restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body.toString())
                        .retrieve()
                        .toBodilessEntity();

                log.info("Telegram message sent successfully ({} chars)", chunk.length());
            } catch (Exception e) {
                log.error("Failed to send Telegram message: {}", e.getMessage());
                allSent = false;
            }
        }
        return allSent;
    }

    private String[] splitMessage(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return new String[]{message};
        }

        java.util.List<String> chunks = new java.util.ArrayList<>();
        String[] lines = message.split("\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (current.length() + line.length() + 1 > MAX_MESSAGE_LENGTH) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            current.append(line).append("\n");
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }

        return chunks.toArray(new String[0]);
    }
}
