package com.ainews.service;

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DigestBot implements SpringLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(DigestBot.class);
    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final TelegramClient telegramClient;
    private final String botToken;

    public DigestBot(TelegramClient telegramClient,
                     @Value("${app.telegram.chat-id:}") String chatId) {
        this.telegramClient = telegramClient;
        this.botToken = System.getenv("TELEGRAM_BOT_TOKEN");
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updates -> {
            // On ne traite pas les messages entrants — on n'envoie que des digests
        };
    }

    public boolean sendDigest(String message, String chatId) {
        if (chatId == null || chatId.isBlank()) {
            log.error("TELEGRAM_CHAT_ID not set");
            return false;
        }

        String[] chunks = splitMessage(message);
        boolean allSent = true;

        for (String chunk : chunks) {
            try {
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text(chunk)
                        .parseMode("Markdown")
                        .build();

                telegramClient.execute(sendMessage);
                log.info("Telegram message sent ({} chars)", chunk.length());
            } catch (TelegramApiException e) {
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

        List<String> chunks = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : message.split("\n")) {
            if (line.length() > MAX_MESSAGE_LENGTH) {
                if (!current.isEmpty()) {
                    chunks.add(current.toString());
                    current = new StringBuilder();
                }
                for (int i = 0; i < line.length(); i += MAX_MESSAGE_LENGTH) {
                    chunks.add(line.substring(i, Math.min(i + MAX_MESSAGE_LENGTH, line.length())));
                }
                continue;
            }

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
