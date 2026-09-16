package com.ainews.model;

import java.time.LocalDate;
import java.util.Objects;

public class NewsItem {

    private String title;
    private String source;
    private LocalDate date;
    private String url;
    private String summary;

    public NewsItem() {
    }

    public NewsItem(String title, String source, LocalDate date, String url, String summary) {
        this.title = title;
        this.source = source;
        this.date = date;
        this.url = url;
        this.summary = summary;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String toRawText() {
        return String.format("[%s] %s — %s\n%s\n%s",
                source, title,
                date != null ? date.toString() : "unknown",
                url != null ? url : "",
                summary != null ? summary : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsItem newsItem = (NewsItem) o;
        return Objects.equals(normalize(title), normalize(newsItem.title));
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalize(title));
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    @Override
    public String toString() {
        return "NewsItem{" + title + " [" + source + "]}";
    }
}
