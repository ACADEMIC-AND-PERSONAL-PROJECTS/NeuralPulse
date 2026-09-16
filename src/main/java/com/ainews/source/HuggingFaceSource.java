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
public class HuggingFaceSource extends AbstractHtmlSource {

    private static final String BASE_URL = "https://huggingface.co";
    private static final String BLOG_URL = BASE_URL + "/blog";

    @Override
    public String getSourceName() {
        return "Hugging Face";
    }

    @Override
    public List<NewsItem> fetchLastWeek() {
        LocalDate since = LocalDate.now().minusDays(7);
        List<NewsItem> items = new ArrayList<>();

        Document doc = fetchDocument(BLOG_URL);
        if (doc == null) return items;

        Elements articles = doc.select("article, div[class*='post'], a[href*='/blog/']");
        for (Element el : articles) {
            try {
                String title = text(el, "h2, h3, h1");
                String url = absUrl(el, "a[href]", BASE_URL);
                String summary = text(el, "p");

                if (title == null || title.isBlank()) continue;

                items.add(new NewsItem(title.trim(), getSourceName(), LocalDate.now(), url, summary));
            } catch (Exception e) {
                log.debug("Error parsing HuggingFace article: {}", e.getMessage());
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
