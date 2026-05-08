# LeetCode Trainer

A personal interview prep tool that recommends daily practice problems, tracks your attempts, and visualizes your progress.

## Features

- **Daily recommendations** — generates a session of problems each morning targeting a configurable solve time (default 90 min), mixing new problems, spaced-repetition reviews, and problems flagged for revisit
- **Attempt tracking** — log each solve with status (Solved / Review / Failed), time taken, and notes
- **Problem browser** — filter the full problem set by difficulty, pattern, or company
- **Stats dashboard** — GitHub-style activity heatmap and per-pattern completion progress bars
- **Import / Export** — import your existing Google Sheets history directly by URL or CSV upload; export all attempts back to CSV
- **AI-agent compatible API** — Swagger docs at `/swagger-ui.html`, all endpoints accept structured parameters for programmatic use

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2 |
| Database | PostgreSQL 15 |
| Frontend | React 18 + TypeScript (Vite) |
| Containerization | Docker + Docker Compose |

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Ports `3000`, `8080`, and `5432` available

### Run with Docker Compose

```bash
git clone <your-repo-url>
cd vibecode-leetcode-trainer
docker compose up -d
```

That's it. On first boot the backend automatically seeds ~200 problems from the [seanprashad/leetcode-patterns](https://github.com/seanprashad/leetcode-patterns) problem set.

| Service | URL |
|---|---|
| Web app | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger docs | http://localhost:8080/swagger-ui.html |

### QNAP Container Station

1. Copy the project folder to your QNAP (via SMB share or `scp`)
2. In Container Station, open a terminal or SSH into the NAS
3. `cd` into the project folder and run `docker compose up -d`
4. Access the app at `http://<qnap-ip>:3000`

Data is persisted in a named Docker volume (`postgres_data`), so it survives container restarts.

### Local development (without Docker)

**Prerequisites:** Java 17+, Maven, Node 20+, a local PostgreSQL instance

**Backend:**
```bash
cd backend
# Set DB connection (or update application.properties)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/leetcode_trainer
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
# App runs at http://localhost:5173
# API calls are proxied to http://localhost:8080
```

## Importing Your Google Sheet

Your existing spreadsheet can be imported directly:

1. Go to **Attempts → 📥 Import / Export → Import**
2. Choose **Google Sheet URL**, paste your sheet link, and click **Import**
   - The sheet must be set to *Anyone with the link can view*
3. Or choose **Upload CSV**, download your sheet as CSV first (File → Download → CSV), then drag-and-drop it

**Expected column format:**
```
Problem | Date       | Time     | Level  | Description | Solution | Tags
--------|------------|----------|--------|-------------|----------|------
Two Sum | 3/15/2026  | 2:51:00  | easy   | ...         | ...      | arrays
3Sum    | 4/27/2026  | DNF      | medium | ...         | ...      |
```

Time is parsed as `MM:SS` (e.g. `25:28:00` → 25 min). `DNF` becomes **Failed**; `DNF` with "review" or "look into" in the solution becomes **Review**. Duplicate rows (same problem + same day) are skipped automatically.

## API Reference

Full interactive docs at `/swagger-ui.html`. Key endpoints:

### Recommendations
```
GET  /api/recommendations/today
     ?targetMinutes=90&difficulty=MEDIUM&pattern=Two+Pointers

POST /api/recommendations/generate
     { "targetMinutes": 60, "forceRegenerate": true, "pattern": "Binary Search" }
```

### Attempts
```
GET  /api/attempts
POST /api/attempts
     { "problemId": 42, "status": "SOLVED", "timeTakenMinutes": 18, "notes": "..." }
```

### Problems
```
GET  /api/problems?difficulty=EASY&pattern=Sliding+Window&company=Google
GET  /api/problems/patterns      ← all distinct patterns
GET  /api/problems/companies     ← all distinct companies
POST /api/problems               ← add a custom problem
```

### Stats
```
GET  /api/stats/heatmap?days=365    ← daily solve counts for heatmap
GET  /api/stats/progress            ← solved / total per pattern
```

### Import / Export
```
POST /api/spreadsheet/import/file   ← multipart CSV upload
POST /api/spreadsheet/import/url    { "url": "https://docs.google.com/spreadsheets/d/..." }
GET  /api/spreadsheet/export        ← download all attempts as CSV
```

## Recommendation Algorithm

Each morning a session is generated to fill the target time:

1. **~25% Review** — problems where your most recent attempt is marked *Review* (highest priority)
2. **~25% Spaced repetition** — problems previously marked *Solved* (oldest first)
3. **Remainder New** — problems never attempted, shuffled within each difficulty tier

Within the session, problems are ordered Easy → Medium → Hard. Estimated solve time uses your recorded times from past attempts; for new problems it defaults to Easy=10 min, Medium=20 min, Hard=30 min.

The session is cached per day. Use `forceRegenerate: true` on the generate endpoint (or the "New Session" button) to get a fresh set.

## Project Structure

```
vibecode-leetcode-trainer/
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/leetcodetrainer/
│       ├── config/          # CORS, Jackson
│       ├── controller/      # REST controllers
│       ├── dto/             # Request / response DTOs
│       ├── model/           # JPA entities
│       ├── repository/      # Spring Data repositories
│       └── service/         # Business logic + seed
└── frontend/
    ├── Dockerfile
    ├── nginx.conf
    └── src/
        ├── api/             # Typed API client
        ├── components/      # Shared UI components
        ├── pages/           # Dashboard, Problems, Attempts, Stats
        └── types/           # TypeScript interfaces
```

## Future Plans

- Discord bot integration for logging attempts via chat
- LLM-assisted attempt recording (describe your solution, AI fills in the form)
