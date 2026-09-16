# AI News Weekly Digest — Spécifications Techniques

## 1. Vue d'ensemble

Application **Spring Boot** qui récupère automatiquement les actualités IA chaque semaine, les résume via un LLM (Groq), et envoie un digest personnalisé sur **Telegram** le samedi matin à 9h.

---

## 2. Stack technique

| Composant | Choix | Justification |
|-----------|-------|---------------|
| **Runtime** | Java 17 (LTS) | Léger, suffisant pour 1GB RAM |
| **Framework** | Spring Boot 3.x | Écosystème riche, Spring AI intégré |
| **LLM** | Groq (Llama 3.3 70B) | Gratuit, ultra-rapide, OpenAI-compatible |
| **Scraping** | JSoup | Parsing HTML robuste en Java |
| **HTTP** | Spring Web RestClient | Appels API officielles |
| **Bot** | TelegramBots (org.telegram:telegrambots) | Library Java officielle pour bots Telegram |
| **BDD** | SQLite (fichier) | Aucun serveur, archivage Markdown |
| **Conteneur** | Docker | Déploiement reproductible sur VPS |
| **Infra** | Oracle Cloud Free Tier (AMD 1/8 OCPU, 1GB RAM) | Gratuit |
| **Secrets** | Variables d'environnement (.env) | Sécurisé, jamais en dur |

---

## 3. Fonctionnalités

### 3.1 Récupération des news

**Sources officielles (API + scraping) :**

| Source | URL | Méthode |
|--------|-----|---------|
| OpenAI Blog | blog.openai.com | Scraping RSS/HTML |
| Anthropic News | anthropic.com/news | Scraping RSS/HTML |
| Google DeepMind | deepmind.google/blog | Scraping RSS/HTML |
| Meta AI | ai.meta.com/blog | Scraping RSS/HTML |
| Mistral AI | mistral.ai/news | Scraping RSS/HTML |
| Hugging Face Blog | huggingface.co/blog | API RSS |

**Newsletters / agrégateurs (scraping) :**

| Source | URL | Méthode |
|--------|-----|---------|
| The Batch | deeplearning.ai/the-batch | Scraping |
| TLDR AI | tldr.tech/ai | Scraping |
| Import AI | importai.substack.com | Scraping RSS |
| The Rundown AI | therundown.ai | Scraping |
| Papers With Code | paperswithcode.com | API/Scraping |
| daily.dev #ai | daily.dev | Scraping |

**Récupération :**
- JSoup pour scraper les pages HTML, extraire titres + résumés + dates
- Spring RestClient pour les API RSS/JSON
- Filtre : uniquement les articles de la dernière semaine (7 jours)
- Dédoublonnage par similarité de titre (Levenshtein ou n-grams)

### 3.2 Traitement par LLM

**Appel Groq :**
- Endpoint : `https://api.groq.com/openai/v1`
- Modèle : `llama-3.3-70b-versatile`
- Gratuit : 30 req/min, 14 400 req/jour
- Auth : clé API via variable d'environnement `GROQ_API_KEY`

**Prompt de traitement :**
```
Tu es un assistant pédagogique spécialisé en IA et Big Data.
Voici les actualités IA de cette semaine :

{news_brutes}

Tu es un étudiant ingénieur en IA & Big Data.
Analyse chaque news et fournis :
1. Un résumé clair et pédagogique (avec analogies si besoin)
2. Pourquoi c'est pertinent pour toi personnellement
3. Classe les 5-10 news les plus importantes de la semaine

Format : Markdown, ton professoral mais accessible.
Si la semaine est calme, dis-le et mentionne 2-3 petites choses à surveiller.
```

### 3.3 Envoi Telegram

**Librairie :** `org.telegram:telegrambots` (v6.x+)

**Fonctionnement :**
- Bot créée via @BotFather
- Token stocké en variable d'environnement `TELEGRAM_BOT_TOKEN`
- Chat ID stocké en variable d'environnement `TELEGRAM_CHAT_ID`
- Le bot envoie le résumé Markdown via `sendMessage` avec `parse_mode=Markdown`

**Format du message :**
```
🤖 AI Weekly Digest — Semaine du {date}

{résumé formaté par le LLM}

---
Sources : OpenAI, Anthropic, DeepMind, ...
Archivé dans : news/week-{date}.md
```

### 3.4 Archivage

- Dossier : `news/`
- Un fichier Markdown par semaine : `news/week-YYYY-MM-DD.md`
- Contenu : résumé complet tel que envoyé sur Telegram
- Créé avant l'envoi Telegram (en cas d'échec, le fichier reste)

### 3.5 Planification

- **Cron Spring** : `@Scheduled(cron = "0 0 9 * * SAT")` — tous les samedis à 9h
- **Timezone** : Europe/Paris (ou timezone du serveur, configurable)
- **Orchestration :**
  1. Fetch toutes les sources en parallèle (CompletableFuture)
  2. Dédoublonner + filtrer par date
  3. Envoyer au LLM pour traitement
  4. Archiver en Markdown
  5. Envoyer sur Telegram
  6. Logger le résultat

### 3.6 Monitoring

- Endpoint : `GET /health` → `{ "status": "ok", "version": "1.0.0", "lastRun": "..." }`
- Logging : SLF4J + Logback, fichier `logs/app.log`
- Pas de métriques lourdes (1GB RAM)

---

## 4. Sécurité

| Règle | Implémentation |
|-------|----------------|
| Pas de secrets en dur | Toutes les clés en variables d'environnement |
| Pas de secrets dans le repo | `.env` dans `.gitignore`, jamais commité |
| Pas de secrets dans les logs | Masquer les tokens dans la config de logging |
| Docker non-root | Container tourne avec user non-root |
| HTTPS pour les appels API | Par défaut avec Spring RestClient |

**Variables d'environnement requises :**
```
GROQ_API_KEY=gsk_xxxxx
TELEGRAM_BOT_TOKEN=123456:ABC-DEF
TELEGRAM_CHAT_ID=-100xxxxxx
APP_PORT=8080
```

---

## 5. Structure du projet

```
ai-news-digest/
├── src/
│   ├── main/
│   │   ├── java/com/ainews/
│   │   │   ├── AiNewsApplication.java
│   │   │   ├── config/
│   │   │   │   ├── AppConfig.java              # Config Spring, schedule
│   │   │   │   └── GroqConfig.java             # Config client Groq
│   │   │   ├── model/
│   │   │   │   └── NewsItem.java               # POJO news (titre, source, date, url, contenu)
│   │   │   ├── service/
│   │   │   │   ├── NewsFetcherService.java      # Scraping + API fetch
│   │   │   │   ├── NewsProcessorService.java    # Appel LLM Groq
│   │   │   │   ├── TelegramService.java         # Envoi message Telegram
│   │   │   │   ├── ArchiveService.java          # Écriture Markdown
│   │   │   │   └── DeduplicationService.java    # Dédoublonnage
│   │   │   ├── source/
│   │   │   │   ├── SourceFetcher.java           # Interface
│   │   │   │   ├── OpenAISource.java
│   │   │   │   ├── AnthropicSource.java
│   │   │   │   ├── DeepMindSource.java
│   │   │   │   ├── MetaAISource.java
│   │   │   │   ├── MistralSource.java
│   │   │   │   ├── HuggingFaceSource.java
│   │   │   │   ├── TheBatchSource.java
│   │   │   │   ├── TLDRAISource.java
│   │   │   │   ├── RundownAISource.java
│   │   │   │   ├── ImportAISource.java
│   │   │   │   ├── PapersWithCodeSource.java
│   │   │   │   └── DailyDevSource.java
│   │   │   ├── scheduler/
│   │   │   │   └── WeeklyDigestScheduler.java   # Cron samedi 9h
│   │   │   └── controller/
│   │   │       └── HealthController.java        # GET /health
│   │   └── resources/
│   │       ├── application.yml                  # Config principale
│   │       ├── application-dev.yml              # Profil dev
│   │       └── application-prod.yml             # Profil prod
│   └── test/
├── news/                                         # Archive Markdown (créé au runtime)
├── Dockerfile
├── docker-compose.yml
├── .env.example                                  # Template des variables d'environnement
├── .gitignore
├── pom.xml
└── specs.md                                      # Ce fichier
```

---

## 6. Docker

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY target/*.jar app.jar
RUN mkdir -p /app/news && chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]
```

### docker-compose.yml
```yaml
version: '3.8'
services:
  ai-news:
    build: .
    container_name: ai-news-digest
    restart: unless-stopped
    ports:
      - "8080:8080"
    env_file:
      - .env
    volumes:
      - ./news:/app/news
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xmx384m
```

---

## 7. Build & Déploiement

```bash
# Build
mvn clean package -DskipTests

# Lancer en dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Lancer en prod (Docker)
docker compose up -d --build

# Vérifier
curl http://localhost:8080/health
```

---

## 8. Contraintes

- **RAM max** : 384MB alloués à Java (1GB total, garder de la marge)
- **CPU** : 1/8 OCPU — pas de traitement lourd en local, tout passe par Groq
- **Pas de dépendance externe lourde** : pas de Kafka, pas de Redis, pas de PostgreSQL
- **Deployment** : SSH sur VPS Oracle + `docker compose up -d`
- **Logs** : rotation journalière, max 10MB par fichier

---

## 9. Architecture & Design Patterns

### Règles architecturales
- **Tous les services doivent avoir une interface** — pas d'implémentation directe en classe concrète
- Le pattern **Strategy** pour les sources de news : chaque source implémente `SourceFetcher`, l'orchestrateur les appelle de manière uniforme
- Le pattern **Factory** ou **Spring DI** pour sélectionner les sources actives
- Le pattern **Template Method** si des sources partagent une logique commune (fetch HTML → parser → extraire)
- Utiliser **Spring profiles** pour switcher entre implémentations (ex: mock en dev, vrai en prod)
- Les dépendances passent par **injection de constructeur**, jamais de `@Autowired` sur les champs

### Exemple de structure attendue
```java
// Interface
public interface SourceFetcher {
    String getSourceName();
    List<NewsItem> fetchLastWeek();
}

// Implémentations
@Component
public class OpenAISource implements SourceFetcher { ... }

@Component
public class AnthropicSource implements SourceFetcher { ... }

// Orchestrateur — ne connaît que l'interface
@Service
public class NewsAggregatorService {
    private final List<SourceFetcher> sources;
    
    public NewsAggregatorService(List<SourceFetcher> sources) {
        this.sources = sources;
    }
}
```

### Anti-patterns interdits
- Classe monolithique qui fait tout
- `new` pour instancier des services (toujours Spring DI)
- Logique métier dans les contrôleurs
- Code non testable à cause de dépendances cachées

## 10. Règles de développement

### Commits
- **Commits récurrents** : faire un commit après chaque fonctionnalité ou module terminé
- Ne JAMAIS tout commit à la fin — chaque étape doit être versionnée
- Messages de commit conventional : `feat:`, `fix:`, `chore:`, `refactor:`, `docs:`
- Exemples :
  - `feat: add OpenAI news source fetcher`
  - `feat: implement Groq LLM service`
  - `feat: add Telegram message sender`
  - `chore: add Dockerfile and docker-compose.yml`
  - `fix: handle empty week gracefully`

### Qualité
- Vérifier que le code compile après chaque changement (`mvn compile`)
- Les erreurs de compilation doivent être corrigées immédiatement
- Pas de TODO laissés dans le code sauf commenté avec ticket

## 10. README

Ne pas créer de README.md sauf si demandé.
