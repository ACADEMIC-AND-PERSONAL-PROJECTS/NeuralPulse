package com.ainews.service;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlackServiceImpl implements SlackService {

    private static final Logger log = LoggerFactory.getLogger(SlackServiceImpl.class);
    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final Slack slack;
    private final String botToken;
    private final String channelId;

    public SlackServiceImpl(@Value("${app.slack.channel-id:}") String channelId) {
        this.slack = Slack.getInstance();
        this.botToken = System.getenv("SLACK_BOT_TOKEN");
        this.channelId = channelId;
    }

    @Override
    public boolean sendMessage(String message) {
        if (channelId == null || channelId.isBlank()) {
            log.warn("SLACK_CHANNEL_ID not set, skipping Slack notification");
            return false;
        }
        if (botToken == null || botToken.isBlank()) {
            log.warn("SLACK_BOT_TOKEN not set, skipping Slack notification");
            return false;
        }

        String[] chunks = splitMessage(message);
        boolean allSent = true;

        for (String chunk : chunks) {
            try {
                ChatPostMessageResponse response = slack.methods(botToken).chatPostMessage(req -> req
                        .channel(channelId)
                        .text(chunk));

                if (response.isOk()) {
                    log.info("Slack message sent ({} chars)", chunk.length());
                } else {
                    log.error("Slack API error: {}", response.getError());
                    allSent = false;
                }
            } catch (SlackApiException e) {
                log.error("Slack API responded with error: {}", e.getMessage());
                allSent = false;
            } catch (IOException e) {
                log.error("Failed to send Slack message: {}", e.getMessage());
                allSent = false;
            }
        }
        return allSent;
    }

    private String[] splitMessage(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return new String[]{message};
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : message.split("\n")) {
            // Si la ligne elle-même dépasse le max, on la découpe en morceaux
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
