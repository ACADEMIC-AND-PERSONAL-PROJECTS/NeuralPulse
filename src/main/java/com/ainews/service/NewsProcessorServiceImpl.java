package com.ainews.service;

import com.ainews.config.GroqConfig;
import com.ainews.model.NewsItem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NewsProcessorServiceImpl implements NewsProcessorService {

    private static final Logger log = LoggerFactory.getLogger(NewsProcessorServiceImpl.class);
    private static final int MAX_ITEMS_FOR_LLM = 40;
    private static final int MAX_CHARS_PER_ITEM = 150;
    private static final int MAX_PROMPT_CHARS = 20000;

    private final RestClient groqRestClient;
    private final GroqConfig groqConfig;
    private final Gson gson = new Gson();

    public NewsProcessorServiceImpl(RestClient groqRestClient, GroqConfig groqConfig) {
        this.groqRestClient = groqRestClient;
        this.groqConfig = groqConfig;
    }

    @Override
    public String processWithLLM(List<NewsItem> newsItems) {
        if (newsItems.isEmpty()) {
            return "Aucune actualité IA majeure cette semaine. Semaine calme !";
        }

        List<NewsItem> selected = selectTopItems(newsItems);
        String newsText = formatNewsForLLM(selected);
        String prompt = buildPrompt(newsText);

        log.info("Sending {} items ({} chars) to Groq model {}", selected.size(), prompt.length(), groqConfig.getModel());

        try {
            JsonObject request = new JsonObject();
            request.addProperty("model", groqConfig.getModel());
            request.addProperty("temperature", 0.7);
            request.addProperty("max_tokens", 4096);

            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", "Tu es un assistant pédagogique spécialisé en IA et Big Data. Tu t'adresses à un étudiant ingénieur.");
            request.add("messages", gson.toJsonTree(List.of(
                    systemMsg,
                    Map.of("role", "user", "content", prompt)
            )));

            String response = groqRestClient.post()
                    .uri("/chat/completions")
                    .body(request.toString())
                    .retrieve()
                    .body(String.class);

            JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
            return responseJson
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            log.error("Error calling Groq API: {}", e.getMessage());
            return buildFallbackDigest(newsItems);
        }
    }

    private List<NewsItem> selectTopItems(List<NewsItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(item -> item.getTitle().length(), Comparator.reverseOrder()))
                .limit(MAX_ITEMS_FOR_LLM)
                .collect(Collectors.toList());
    }

    private String formatNewsForLLM(List<NewsItem> items) {
        StringBuilder sb = new StringBuilder();
        for (NewsItem item : items) {
            String title = truncate(item.getTitle(), MAX_CHARS_PER_ITEM);
            String source = item.getSource();
            String line = String.format("[%s] %s", source, title);
            if (sb.length() + line.length() + 2 > MAX_PROMPT_CHARS) break;
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String buildPrompt(String newsText) {
        return """
                Voici les actualités IA de cette semaine :

                %s

                Analyse les plus importantes et fournis :
                1. Un résumé clair et pédagogique des 5-10 news les plus marquantes
                2. Pourquoi c'est pertinent pour un étudiant ingénieur en IA & Big Data

                Format : Markdown, ton professoral mais accessible.
                Si la semaine est calme, dis-le et mentionne 2-3 petites choses à surveiller.
                """.formatted(newsText);
    }

    private String buildFallbackDigest(List<NewsItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("## News de la semaine\n\n");
        int count = 0;
        for (NewsItem item : items) {
            if (count >= 10) break;
            sb.append(String.format("- **%s** (%s)\n", item.getTitle(), item.getSource()));
            count++;
        }
        sb.append("\n*Digest généré sans LLM (erreur API)*");
        return sb.toString();
    }
}
