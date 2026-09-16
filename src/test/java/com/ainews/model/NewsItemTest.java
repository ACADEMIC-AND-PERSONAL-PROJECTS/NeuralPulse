package com.ainews.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class NewsItemTest {

    @Test
    void shouldCreateNewsItemWithAllFields() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        NewsItem item = new NewsItem("Test Title", "OpenAI", date, "https://example.com", "Summary");

        assertEquals("Test Title", item.getTitle());
        assertEquals("OpenAI", item.getSource());
        assertEquals(date, item.getDate());
        assertEquals("https://example.com", item.getUrl());
        assertEquals("Summary", item.getSummary());
    }

    @Test
    void shouldHaveDefaultConstructor() {
        NewsItem item = new NewsItem();
        assertNull(item.getTitle());
        assertNull(item.getSource());
    }

    @Test
    void shouldSetAndGetFields() {
        NewsItem item = new NewsItem();
        item.setTitle("New Title");
        item.setSource("Anthropic");
        item.setDate(LocalDate.now());
        item.setUrl("https://test.com");
        item.setSummary("Test summary");

        assertEquals("New Title", item.getTitle());
        assertEquals("Anthropic", item.getSource());
    }

    @Test
    void shouldBeEqualByNormalizedTitle() {
        NewsItem item1 = new NewsItem("OpenAI Releases GPT-5", "OpenAI", LocalDate.now(), null, null);
        NewsItem item2 = new NewsItem("OpenAI Releases GPT-5!", "Source2", LocalDate.now(), null, null);

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    void shouldNotBeEqualByDifferentTitle() {
        NewsItem item1 = new NewsItem("OpenAI Releases GPT-5", "OpenAI", LocalDate.now(), null, null);
        NewsItem item2 = new NewsItem("Anthropic Releases Claude 4", "Anthropic", LocalDate.now(), null, null);

        assertNotEquals(item1, item2);
    }

    @Test
    void shouldFormatRawText() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        NewsItem item = new NewsItem("Title", "OpenAI", date, "https://example.com", "Summary text");

        String raw = item.toRawText();

        assertTrue(raw.contains("OpenAI"));
        assertTrue(raw.contains("Title"));
        assertTrue(raw.contains("2024-01-15"));
        assertTrue(raw.contains("https://example.com"));
        assertTrue(raw.contains("Summary text"));
    }

    @Test
    void shouldHandleNullFields() {
        NewsItem item = new NewsItem(null, null, null, null, null);
        String raw = item.toRawText();
        assertNotNull(raw);
        assertTrue(raw.contains("unknown"));
    }

    @Test
    void shouldCompareIgnoringPunctuationAndCase() {
        NewsItem item1 = new NewsItem("Hello, World!", "src", LocalDate.now(), null, null);
        NewsItem item2 = new NewsItem("hello world", "src", LocalDate.now(), null, null);
        assertEquals(item1, item2);
    }
}
