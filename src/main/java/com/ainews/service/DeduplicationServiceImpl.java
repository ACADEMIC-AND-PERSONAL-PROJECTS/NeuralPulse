package com.ainews.service;

import com.ainews.model.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeduplicationServiceImpl implements DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationServiceImpl.class);

    private static final int LEVENSHTEIN_THRESHOLD = 3;

    @Override
    public List<NewsItem> deduplicate(List<NewsItem> items) {
        Map<String, NewsItem> uniqueMap = new LinkedHashMap<>();

        for (NewsItem item : items) {
            String key = normalizeTitle(item.getTitle());
            boolean isDuplicate = false;

            for (String existingKey : uniqueMap.keySet()) {
                if (levenshteinDistance(key, existingKey) <= LEVENSHTEIN_THRESHOLD) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                uniqueMap.put(key, item);
            }
        }

        List<NewsItem> result = new ArrayList<>(uniqueMap.values());
        log.info("Deduplication: {} items -> {} unique items", items.size(), result.size());
        return result;
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
