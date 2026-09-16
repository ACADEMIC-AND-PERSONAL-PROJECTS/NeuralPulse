package com.ainews.source;

import com.ainews.model.NewsItem;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PapersWithCodeSource extends AbstractHtmlSource {

    private static final String BASE_URL = "https://paperswithcode.com";
    private static final String LATEST_URL = BASE_URL;

    @Override
    public String getSourceName() {
        return "Papers With Code";
    }

    @Override
    public List<NewsItem> fetchLastWeek() {
        LocalDate since = LocalDate.now().minusDays(7);
        List<NewsItem> items = new ArrayList<>();

        Document doc = fetchDocument(LATEST_URL);
        if (doc == null) return items;

        Elements articles = doc.select("div[class*='paper'], div[class*='item'], a[href*='/paper/']");
        for (Element el : articles) {
            try {
                String title = text(el, "h1, h2, h3, a");
                String url = absUrl(el, "a[href]", BASE_URL);
                String summary = text(el, "p, div[class*='abstract']");

                if (title == null || title.isBlank()) continue;

                items.add(new NewsItem(title.trim(), getSourceName(), LocalDate.now(), url, summary));
            } catch (Exception e) {
                log.debug("Error parsing Papers With Code item: {}", e.getMessage());
            }
        }
        return items;
    }

    @Override
    protected String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    protected String getArticleSelector() {
        return "div[class*='paper']";
    }

    @Override
    protected String getTitleSelector(Element article) {
        return null;
    }

    @Override
    protected String getUrlSelector(Element article) {
        return null;
    }

    @Override
    protected String getDateSelector(Element article) {
        return null;
    }

    @Override
    protected String getSummarySelector(Element article) {
        return null;
    }
}
