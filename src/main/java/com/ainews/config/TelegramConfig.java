package com.ainews.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient() {
        String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
        return new OkHttpTelegramClient(botToken);
    }
}
