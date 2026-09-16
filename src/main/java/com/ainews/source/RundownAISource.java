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
public class RundownAISource extends AbstractHtmlSource {

    private static final String BASE_URL = "https://www.therundown.ai";
    private static final String NEWS_URL = BASE_URL;

    @Override
    public String getSourceName() {
        return "The Rundown AI";
    }

    @Override
    public List<NewsItem> fetchLastWeek() {
        LocalDate since = LocalDate.now().minusDays(7);
        List<NewsItem> items = new ArrayList<>();

        Document doc = fetchDocument(NEWS_URL);
        if (doc == null) return items;

        Elements articles = doc.select("article, div[class*='post'], div[class*='issue'], a[href*='/p/']");
        for (Element el : articles) {
            try {
                String title = text(el, "h2, h3, h1");
                String url = absUrl(el, "a[href]", BASE_URL);
                String summary = text(el, "p");

                if (title == null || title.isBlank()) continue;

                items.add(new NewsItem(title.trim(), getSourceName(), LocalDate.now(), url, summary));
            } catch (Exception e) {
                log.debug("Error parsing Rundown AI article: {}", e.getMessage());
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
        return "article";
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
