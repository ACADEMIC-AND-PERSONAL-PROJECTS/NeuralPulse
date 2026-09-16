package com.ainews.service;

import com.ainews.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicationServiceTest {

    private DeduplicationServiceImpl deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new DeduplicationServiceImpl();
    }

    @Test
    void shouldRemoveExactDuplicates() {
        LocalDate date = LocalDate.now();
        List<NewsItem> items = List.of(
                new NewsItem("OpenAI Releases GPT-5", "OpenAI", date, "url1", "summary1"),
                new NewsItem("OpenAI Releases GPT-5", "Source2", date, "url2", "summary2"),
                new NewsItem("Anthropic Releases Claude", "Anthropic", date, "url3", "summary3")
        );

        List<NewsItem> result = deduplicationService.deduplicate(items);

        assertEquals(2, result.size());
    }

    @Test
    void shouldRemoveSimilarTitles() {
        LocalDate date = LocalDate.now();
        List<NewsItem> items = List.of(
                new NewsItem("OpenAI announces new model", "OpenAI", date, "url1", "s1"),
                new NewsItem("OpenAI announces new models", "Source2", date, "url2", "s2")
        );

        List<NewsItem> result = deduplicationService.deduplicate(items);

        assertEquals(1, result.size());
    }

    @Test
    void shouldKeepDistinctTitles() {
        LocalDate date = LocalDate.now();
        List<NewsItem> items = List.of(
                new NewsItem("OpenAI Releases GPT-5", "OpenAI", date, "url1", "s1"),
                new NewsItem("Anthropic Releases Claude 4", "Anthropic", date, "url2", "s2"),
                new NewsItem("Google Announces Gemini Ultra", "Google", date, "url3", "s3")
        );

        List<NewsItem> result = deduplicationService.deduplicate(items);

        assertEquals(3, result.size());
    }

    @Test
    void shouldHandleEmptyList() {
        List<NewsItem> result = deduplicationService.deduplicate(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleSingleItem() {
        List<NewsItem> items = List.of(
                new NewsItem("Title", "Source", LocalDate.now(), "url", "summary")
        );

        List<NewsItem> result = deduplicationService.deduplicate(items);

        assertEquals(1, result.size());
    }

    @Test
    void shouldNormalizeBeforeComparing() {
        LocalDate date = LocalDate.now();
        List<NewsItem> items = List.of(
                new NewsItem("Hello, World!", "src", date, "url1", "s1"),
                new NewsItem("Hello World", "src", date, "url2", "s2"),
                new NewsItem("HELLO WORLD", "src", date, "url3", "s3")
        );

        List<NewsItem> result = deduplicationService.deduplicate(items);

        assertEquals(1, result.size());
    }
}
