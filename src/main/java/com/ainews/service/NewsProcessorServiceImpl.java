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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NewsProcessorServiceImpl implements NewsProcessorService {

    private static final Logger log = LoggerFactory.getLogger(NewsProcessorServiceImpl.class);

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

        String newsText = newsItems.stream()
                .map(NewsItem::toRawText)
                .collect(Collectors.joining("\n\n"));

        String prompt = buildPrompt(newsText);

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
            return "Erreur lors du traitement par le LLM. News brutes :\n\n" + newsText;
        }
    }

    private String buildPrompt(String newsText) {
        return """
                Voici les actualités IA de cette semaine :

                %s

                Analyse chaque news et fournis :
                1. Un résumé clair et pédagogique (avec analogies si besoin)
                2. Pourquoi c'est pertinent pour un étudiant ingénieur en IA & Big Data
                3. Classe les 5-10 news les plus importantes de la semaine

                Format : Markdown, ton professoral mais accessible.
                Si la semaine est calme, dis-le et mentionne 2-3 petites choses à surveiller.
                """.formatted(newsText);
    }
}
