package com.ainews.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TelegramServiceImpl implements TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramServiceImpl.class);

    private final DigestBot digestBot;
    private final String chatId;

    public TelegramServiceImpl(DigestBot digestBot,
                               @Value("${app.telegram.chat-id:}") String chatId) {
        this.digestBot = digestBot;
        this.chatId = chatId;
    }

    @Override
    public boolean sendMessage(String message) {
        return digestBot.sendDigest(message, chatId);
    }
}
