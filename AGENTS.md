# Repository Guidelines

## Project Structure & Module Organization

This repository contains a Java 21 Spring Boot backend and a React/Vite frontend. Backend source lives in `src/main/java/xyz/zlov/app/strava`, organized by feature packages such as `activity`, `auth`, `fit`, `stats`, `strava`, and `sync`. Database migrations are in `src/main/resources/db/migration` and use Flyway names like `V9__describe_change.sql`. Tests mirror the main package under `src/test/java`; FIT fixtures live in `src/test/resources/fit`.

Frontend code is in `frontend/src`: reusable UI in `components`, route views in `pages`, API helpers in `api`, and static assets in `assets` or `public`.

## Build, Test, and Development Commands

- `docker compose up -d`: starts PostgreSQL.
- `./mvnw spring-boot:run`: runs the backend locally.
- `./mvnw test`: runs backend JUnit/Spring tests.
- `./mvnw package`: builds the backend artifact and runs tests.
- `cd frontend && npm install`: installs frontend dependencies.
- `cd frontend && npm run dev`: starts the Vite dev server.
- `cd frontend && npm run build`: creates the production bundle in `frontend/dist`.
- `cd frontend && npm run lint`: runs ESLint for JS/JSX files.

## Coding Style & Naming Conventions

Use the existing Java package-by-feature layout. Java classes use `PascalCase`; methods and fields use `camelCase`; DTOs keep the `Dto` suffix used in `stats/dto` and `activity/dto`. Keep Spring stereotypes close to their domain package, for example controllers in `fit` or `sync`.

Frontend JSX components use `PascalCase` filenames such as `ActivityCharts.jsx`; utility modules use lower camel case such as `client.js`. Follow the current two-space JavaScript style and run ESLint before submitting frontend changes.

## Testing Guidelines

Backend tests use JUnit 5 with Spring Boot test support. Name focused tests after the unit under test, for example `FitParserTest`, and place fixtures under `src/test/resources`. Add tests for FIT parsing, statistics calculations, persistence mappings, and API behavior. No frontend test runner is configured; validate frontend changes with `npm run lint` and `npm run build`.

## Commit & Pull Request Guidelines

Recent history mixes Conventional Commit prefixes (`feat:`, `docs:`, `perf:`) with short descriptive Russian messages. Prefer `type: concise summary` for new commits when practical, for example `feat: add swim pace chart`.

Pull requests should include a short description, test commands run, linked issue if applicable, and screenshots for visible UI changes. Mention database migrations, new environment variables, or Strava/FIT import changes explicitly.

## Security & Configuration Tips

Do not commit real Strava credentials or local secrets. Configure `STRAVA_CLIENT_ID`, `STRAVA_CLIENT_SECRET`, `STRAVA_REDIRECT_URI`, `STRAVA_ATHLETE_ID`, and `APP_CORS_ALLOWED_ORIGINS` through the environment or local run configuration.
