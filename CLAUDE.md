# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Personal Strava dashboard: a Java 21 / Spring Boot 4 backend (PostgreSQL, Flyway, Garmin FIT SDK) and a React 19 / Vite / Tailwind 4 frontend. See `AGENTS.md` for contributor conventions and `README.md` (Russian) for the full feature tour.

## Commands

Backend (run from repo root):
- `docker compose up -d` — start PostgreSQL (required before running the backend; app uses `ddl-auto: validate`, so schema comes only from Flyway).
- `./mvnw spring-boot:run` — run backend on `:8080`. Needs `STRAVA_CLIENT_ID` / `STRAVA_CLIENT_SECRET` in the environment.
- `./mvnw test` — run all backend tests.
- `./mvnw test -Dtest=FitParserTest` — run a single test class (or `-Dtest=FitParserTest#methodName`).
- `./mvnw package` — build the jar (runs tests).

Frontend (run from `frontend/`):
- `npm install`, then `npm run dev` — Vite dev server on `:5173`, proxies `/api` and `/auth` to `:8080`.
- `npm run lint` — ESLint. `npm run build` — production bundle to `frontend/dist`. There is **no frontend test runner**; validate with lint + build.

Full stack via Docker: `docker compose up` builds backend, frontend (nginx on `:80`), and Postgres.

## Architecture

**Two data ingestion paths write into the same `activities` table** — everything downstream is source-agnostic:

1. **Strava API sync** (`strava/`): `StravaPoller` runs `@Scheduled(fixedDelay = 1h)` and is triggerable via `POST /api/sync/full`. It pages `/athlete/activities` through `StravaClient` and upserts summary rows. `SyncState` (per-athlete) tracks status/last-sync/error. Incremental sync uses `MAX(start_date)` as the `after` cursor; full sync uses `after=0`.

2. **FIT import** (`fit/`): `FitImportService` + `FitParser` (Garmin FIT SDK) parse an uploaded `.fit`/`.fit.gz` into an `Activity` with the **same fields the Strava API path produces**, including the raw-JSON detail caches. Entry point `POST` on `ImportController`; frontend page is `/import`. This path needs **no Strava API access** — it exists because Strava disabled API access for apps without a paid subscription (`403 Inactive`). The app stays fully functional on accumulated data plus FIT imports.

**Lazy, permanently-cached activity details.** The `Activity` entity stores summary columns plus three `jsonb` blobs: `activity_raw`, `laps_raw`, `streams_raw` (map track lives in `map_polyline`). On first open of an activity's detail tabs, `ActivityService.fetchAndCacheDetails` / `fetchAndCacheStreams` fetch from Strava once and persist forever — subsequent opens cost no API calls. FIT import fills these same blobs directly at import time.

**Stats** (`stats/`): `StatsService` delegates to native/JPQL queries in `ActivityRepository` returning `Object[]` rows, mapped into `*Dto` records. Speed conversions (m/s → km/h) happen in the service layer.

**Auth** (`auth/`): OAuth2 with Strava. `TokenService` stores/refreshes the token in `oauth_tokens` and exposes `getValidAccessToken()` / `getAthleteId()`. Browse to `/auth/strava` to authorize; `client.js` redirects there on any `401`. If no token and no activities exist, `strava.athlete-id` config supplies the athlete id for FIT import.

**Backend layout** is package-by-feature under `xyz.zlov.app.strava`: `activity`, `auth`, `config`, `dashboard` (REST `/api` aggregator), `fit`, `stats`, `strava`, `sync`. Controllers/services/repos stay in their feature package; DTOs are records with a `Dto` suffix in `*/dto`.

**Frontend**: `pages/Dashboard.jsx` and `pages/ImportPage.jsx` are the two views; `components/` holds the charts (Recharts) and the Leaflet map; `api/client.js` is the single axios layer. Deep-linking: opening an activity sets `?activity={stravaId}` so links reopen the modal directly.

## Conventions & gotchas

- **Schema changes go through Flyway only** (`src/main/resources/db/migration`, `V{n}__description.sql`). `ddl-auto` is `validate` — entity changes without a matching migration fail startup. Bump the version number; never edit an applied migration.
- **CORS**: browser `POST`s (FIT import) require the frontend origin in `APP_CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`; set the real domain in prod or imports 403).
- **FIT filename = Strava activity id** (that's how Strava bulk-export names files), or pass `stravaId` explicitly. Import upserts by `strava_id`.
- `SHOW_ACTIVITY_MAP` is baked into the frontend build (`VITE_SHOW_ACTIVITY_MAP` build arg), not a runtime toggle.
- Backend tests: JUnit 5 + Spring Boot test; FIT fixtures in `src/test/resources/fit`.
