package com.ainews.scheduler;

import com.ainews.model.NewsItem;
import com.ainews.service.ArchiveService;
import com.ainews.service.DeduplicationService;
import com.ainews.service.NewsFetcherService;
import com.ainews.service.NewsProcessorService;
import com.ainews.service.TelegramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class WeeklyDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDigestScheduler.class);

    private final NewsFetcherService newsFetcherService;
    private final DeduplicationService deduplicationService;
    private final NewsProcessorService newsProcessorService;
    private final ArchiveService archiveService;
    private final TelegramService telegramService;
    private final ZoneId appTimezone;

    public WeeklyDigestScheduler(NewsFetcherService newsFetcherService,
                                  DeduplicationService deduplicationService,
                                  NewsProcessorService newsProcessorService,
                                  ArchiveService archiveService,
                                  TelegramService telegramService,
                                  ZoneId appTimezone) {
        this.newsFetcherService = newsFetcherService;
        this.deduplicationService = deduplicationService;
        this.newsProcessorService = newsProcessorService;
        this.archiveService = archiveService;
        this.telegramService = telegramService;
        this.appTimezone = appTimezone;
    }

    @Scheduled(cron = "0 0 9 * * SAT", zone = "Europe/Paris")
    public void executeWeeklyDigest() {
        log.info("=== Starting weekly digest ===");

        LocalDate now = LocalDate.now(appTimezone);
        String weekDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try {
            // 1. Fetch all sources in parallel
            List<NewsItem> rawNews = newsFetcherService.fetchAllSources();
            log.info("Raw news fetched: {}", rawNews.size());

            // 2. Deduplicate + filter
            List<NewsItem> uniqueNews = deduplicationService.deduplicate(rawNews);
            log.info("Unique news after dedup: {}", uniqueNews.size());

            // 3. Process with LLM
            String digest = newsProcessorService.processWithLLM(uniqueNews);

            // 4. Build full message
            String fullMessage = buildMessage(digest, weekDate);

            // 5. Archive to Markdown (before Telegram, so file exists even if send fails)
            archiveService.archiveWeeklyDigest(fullMessage, weekDate);

            // 6. Send to Telegram
            boolean sent = telegramService.sendMessage(fullMessage);
            if (sent) {
                log.info("Weekly digest sent to Telegram successfully");
            } else {
                log.warn("Failed to send digest to Telegram");
            }

        } catch (Exception e) {
            log.error("Error executing weekly digest: {}", e.getMessage(), e);
        }

        log.info("=== Weekly digest completed ===");
    }

    private String buildMessage(String digest, String weekDate) {
        return """
                🤖 AI Weekly Digest — Semaine du %s

                %s

                ---
                Sources : OpenAI, Anthropic, DeepMind, Meta AI, Mistral, Hugging Face, The Batch, TLDR AI, Import AI, The Rundown AI, Papers With Code, daily.dev
                Archivé dans : news/week-%s.md
                """.formatted(weekDate, digest, weekDate);
    }
}
