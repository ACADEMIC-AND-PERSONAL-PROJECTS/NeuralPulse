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
    private static final int MAX_ITEMS_FOR_LLM = 25;
    private static final int MAX_CHARS_PER_ITEM = 200;
    private static final int MAX_PROMPT_CHARS = 12000;

    private static final String SYSTEM_PROMPT = """
            Tu es un professeur pédagogique spécialisé en Intelligence Artificielle et Big Data.
            Tu t'adresses à un étudiant ingénieur en 4e année, spécialisé en IA & Big Data.
            L'année prochaine, il suivra des cours de Deep Learning avancé (CNN, RNN, Transformers,
            fine-tuning, entraînement de modèles).

            TON RÔLE :
            - Fais le lien entre chaque actualité et le parcours concret de cet étudiant
            - Utilise des analogies concrètes pour expliquer les concepts techniques
            - Relie les news à ce qu'il a déjà appris (ML classique, Python, data engineering)
            - Prépare-le mentalement aux cours de Deep Learning de l'année prochaine

            STYLE :
            - Tutoiement (tu es un mentor, pas un prof distant)
            - Ton chaleureux mais rigoureux
            - Pas de jargon inutile, mais ne surplombe pas non plus

            FORMAT DE SORTIE :
            - Utilise uniquement du texte brut, PAS de Markdown brut (pas de #, ##, **, *, ```)
            - Pour les titres : utilise des EMOJI comme séparateurs visuels (ex: 🚀 Nom du titre)
            - Pour les listes : des tirets (-) ou des puces
            - Pour l'accent : utilise des MAJUSCULES ou des emojis plutôt que du gras
            - Mets les liens sources sur une ligne séparée avec "Lire plus : <url>"
            - Maximum 10 news analysées, pas plus
            - Reste concis : chaque news = 2-3 lignes max
            """;

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
        String userPrompt = buildUserPrompt(newsText);

        log.info("Sending {} items ({} chars) to Groq model {}", selected.size(), userPrompt.length(), groqConfig.getModel());

        try {
            JsonObject request = new JsonObject();
            request.addProperty("model", groqConfig.getModel());
            request.addProperty("temperature", 0.7);
            request.addProperty("max_tokens", 1000);

            request.add("messages", gson.toJsonTree(List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userPrompt)
            )));

            String response = groqRestClient.post()
                    .uri("/chat/completions")
                    .body(request.toString())
                    .retrieve()
                    .body(String.class);

            JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
            String content = responseJson
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            return cleanMarkdownForTelegram(content);

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
            String url = item.getUrl() != null ? item.getUrl() : "pas de lien";
            String line = String.format("[%s] %s | %s", source, title, url);
            if (sb.length() + line.length() + 2 > MAX_PROMPT_CHARS) break;
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String buildUserPrompt(String newsText) {
        return """
                Voici les actualités IA récupérées cette semaine (avec sources) :

                %s

                Pour chaque news importante, fournis :
                - Un résumé clair et pédagogique (2-3 lignes)
                - Pourquoi c'est pertinent pour ton parcours d'ingénieur en IA & Big Data
                - Le lien vers l'article source

                Classe les 5-10 plus importantes. Reste concis.
                Si c'est une semaine calme, dis-le et mentionne 2-3 choses à surveiller.
                """.formatted(newsText);
    }

    private String cleanMarkdownForTelegram(String text) {
        String cleaned = text;
        cleaned = cleaned.replaceAll("^#{1,6}\\s+", "");
        cleaned = cleaned.replaceAll("\\*\\*(.+?)\\**", "$1");
        cleaned = cleaned.replaceAll("\\*(.+?)\\*", "$1");
        cleaned = cleaned.replaceAll("```[a-z]*\\n?", "");
        cleaned = cleaned.replaceAll("```", "");
        return cleaned.trim();
    }

    private String buildFallbackDigest(List<NewsItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(" NEWS DE LA SEMAINE\n\n");
        int count = 0;
        for (NewsItem item : items) {
            if (count >= 10) break;
            sb.append(String.format(" %s — %s\n", item.getTitle(), item.getSource()));
            if (item.getUrl() != null) {
                sb.append(String.format("   Lire plus : %s\n", item.getUrl()));
            }
            sb.append("\n");
            count++;
        }
        sb.append("\nDigest généré sans LLM (erreur API)");
        return sb.toString();
    }
}
