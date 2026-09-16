package com.ainews.source;

import com.ainews.model.NewsItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractHtmlSource implements SourceFetcher {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected abstract String getBaseUrl();

    protected abstract String getArticleSelector();

    protected abstract String getTitleSelector(Element article);

    protected abstract String getUrlSelector(Element article);

    protected abstract String getDateSelector(Element article);

    protected abstract String getSummarySelector(Element article);

    protected LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        String cleaned = dateStr.trim();
        String[] patterns = {
                "yyyy-MM-dd", "MMM dd, yyyy", "MMMM dd, yyyy",
                "dd MMM yyyy", "dd MMMM yyyy", "MMM d, yyyy",
                "MMMM d, yyyy", "d MMM yyyy", "d MMMM yyyy"
        };
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }
        return LocalDate.now();
    }

    protected Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; AINewsBot/1.0)")
                    .timeout(15000)
                    .get();
        } catch (IOException e) {
            log.warn("Failed to fetch {}: {}", url, e.getMessage());
            return null;
        }
    }

    protected List<NewsItem> scrapeArticles(String url, LocalDate since) {
        List<NewsItem> items = new ArrayList<>();
        Document doc = fetchDocument(url);
        if (doc == null) {
            return items;
        }
        Elements articles = doc.select(getArticleSelector());
        for (Element article : articles) {
            try {
                String title = extractText(getTitleSelector(article));
                String articleUrl = extractAttr(getUrlSelector(article), "href", url);
                String dateStr = extractText(getDateSelector(article));
                String summary = extractText(getSummarySelector(article));

                if (title == null || title.isBlank()) {
                    continue;
                }

                LocalDate date = parseDate(dateStr);
                if (date.isBefore(since)) {
                    continue;
                }

                items.add(new NewsItem(title.trim(), getSourceName(), date, articleUrl, summary));
            } catch (Exception e) {
                log.debug("Error parsing article from {}: {}", getSourceName(), e.getMessage());
            }
        }
        return items;
    }

    private String extractText(String selector) {
        if (selector == null) return null;
        Document doc = Jsoup.parse(selector);
        Elements els = doc.select("*");
        return els.isEmpty() ? selector : els.first().text();
    }

    private String extractAttr(String selector, String attr, String baseUrl) {
        if (selector == null) return null;
        Document doc = Jsoup.parse(selector);
        Elements els = doc.select("[" + attr + "]");
        if (els.isEmpty()) return null;
        String href = els.first().attr(attr);
        if (href.startsWith("/")) {
            return baseUrl.replaceAll("/$", "") + href;
        }
        return href;
    }

    protected String absUrl(Element el, String selector, String baseUrl) {
        Element child = el.selectFirst(selector);
        if (child == null) return null;
        String href = child.attr("href");
        if (href.isEmpty()) return null;
        if (href.startsWith("/")) {
            return baseUrl.replaceAll("/$", "") + href;
        }
        return href;
    }

    protected String text(Element el, String selector) {
        Element child = el.selectFirst(selector);
        return child != null ? child.text() : null;
    }
}
