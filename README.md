# NeuralPulse — AI News Weekly Digest

Pipeline automatique qui scrappe 12 sources IA chaque semaine, les résume via Groq LLM, et envoie un digest pédagogique en français sur Telegram.

## Architecture

**[Vue interactive de l'architecture](docs/architecture.html)** — diagramme animé avec pan/zoom, thème clair/sombre, et vues guidées.

```
Cron (SAMEDI 9h Paris)
  → 12 Sources IA (JSoup HTTP)     ← OpenAI, Anthropic, DeepMind, Meta AI, Mistral,
  → NewsFetcherService              HuggingFace, The Batch, TLDR AI, Import AI,
    → DeduplicationService           The Rundown AI, Papers With Code, daily.dev
      → NewsProcessorService
        → Groq LLM (qwen3.8-27b)
      → ArchiveService → news/week-YYYY-MM-DD.md
    → TelegramService → Telegram Bot API
```

## Stack

| Composant | Technologie |
|-----------|-------------|
| Runtime | Java 17 JRE |
| Framework | Spring Boot 3.3.11 |
| Scraping | JSoup 1.18.1 |
| LLM | Groq API — qwen/qwen3.8-27b |
| Bot | TelegramBots 10.3.0 (long-polling) |
| BDD | SQLite (sqlite-jdbc 3.53.4.0) |
| Infra | Oracle Cloud VPS — 1 OCPU, 1GB RAM |
| Deploy | systemd (pas Docker) |

## Deployment

```bash
# Build
mvn clean package -DskipTests

# Sur le VPS
sudo mkdir -p /opt/ai-news-digest && sudo chown ubuntu:ubuntu /opt/ai-news-digest
scp target/ai-news-digest-1.0.0.jar ubuntu@170.9.35.21:/opt/ai-news-digest/

# Service
sudo systemctl enable ai-news-digest
sudo systemctl start ai-news-digest

# Logs
sudo journalctl -u ai-news-digest -f
```

## Endpoints

| Route | Méthode | Description |
|-------|---------|-------------|
| `/health` | GET | État du service + dernière exécution |
| `/api/test-digest` | POST | Déclenche un digest manuel |

## Configuration

Variables d'environnement (`.env`) :

```
TELEGRAM_BOT_TOKEN=...
TELEGRAM_CHAT_ID=...
GROQ_API=...
```

## Sources scrapées

| Source | Type | Régularité |
|--------|------|------------|
| OpenAI Blog | Blog officiel | Hebdo |
| Anthropic Research | Blog officiel | Hebdo |
| Google DeepMind | Blog officiel | Hebdo |
| Meta AI | Blog officiel | Hebdo |
| Mistral AI | Blog officiel | Hebdo |
| Hugging Face | Blog + Papers | Quotidien |
| The Batch (Andrew Ng) | Newsletter | Hebdo |
| TLDR AI | Newsletter | Quotidien |
| Import AI (Jack Clark) | Newsletter | Hebdo |
| The Rundown AI | Newsletter | Quotidien |
| Papers With Code | Papers trending | Quotidien |
| daily.dev | Agrégateur | Quotidien |

## License

Projet personnel — étudiant en IA & Big Data.
