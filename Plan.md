Вот полный файл — скопируй всё между линиями:

# Strava Dashboard — план реализации

## Стек

| Слой | Технология |
|---|---|
| Backend | Java 21, Spring Boot 3.x |
| БД | PostgreSQL 15+, Flyway (миграции) |
| ORM | Spring Data JPA + Hibernate |
| HTTP клиент | Spring RestClient |
| Сборка | Maven |
| Frontend | React 18 + Recharts |
| Dev окружение | Docker Compose (PostgreSQL локально) |

---

## Структура проекта
strava-dashboard/
├── src/main/java/com/example/strava/
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── TokenService.java
│   │   └── OAuthToken.java
│   ├── strava/
│   │   ├── StravaClient.java
│   │   ├── StravaPoller.java
│   │   └── dto/
│   │       └── StravaActivityDto.java
│   ├── activity/
│   │   ├── Activity.java
│   │   ├── ActivityRepository.java
│   │   └── ActivityService.java
│   ├── stats/
│   │   ├── StatsService.java
│   │   └── dto/
│   ├── dashboard/
│   │   └── DashboardController.java
│   └── sync/
│       ├── SyncState.java
│       └── SyncStateRepository.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__create_oauth_tokens.sql
│       ├── V2__create_activities.sql
│       └── V3__create_sync_state.sql
├── frontend/
│   └── src/
│       ├── components/
│       │   ├── SummaryCards.jsx
│       │   ├── WeeklyVolumeChart.jsx
│       │   ├── PaceChart.jsx
│       │   ├── ActivityCalendar.jsx
│       │   └── ActivityTypeBreakdown.jsx
│       ├── pages/
│       │   └── Dashboard.jsx
│       └── api/
│           └── client.js
├── docker-compose.yml
└── PLAN.md

---

## Фазы реализации

### Фаза 1 — Инфраструктура
**Цель:** проект запускается, БД поднимается, миграции применяются.

- [ ] Создать проект на start.spring.io
    - Зависимости: Spring Web, Spring Data JPA, Spring Security,
      PostgreSQL Driver, Flyway Migration, Lombok, Spring Boot DevTools
- [ ] Настроить application.yml
- [ ] Создать docker-compose.yml с PostgreSQL
- [ ] Написать Flyway миграции V1–V3
- [ ] Проверить: mvn spring-boot:run стартует без ошибок

---

### Фаза 2 — OAuth авторизация
**Цель:** пройти полный OAuth flow, токен сохранён в БД.

- [ ] Зарегистрировать приложение на strava.com/settings/api
    - Callback URL: http://localhost:8080/auth/callback
    - Получить client_id и client_secret
- [ ] AuthController
    - GET /auth/strava — редирект на Strava
    - GET /auth/callback — обмен кода на токен, сохранить в БД
- [ ] TokenService
    - getValidAccessToken() — проверяет expires_at, рефрешит если нужно
    - saveToken() — сохраняет/обновляет в БД
- [ ] Проверить: браузер → /auth/strava → авторизация → токен в БД

---

### Фаза 3 — Strava Client и синхронизация
**Цель:** загрузить все исторические активности в БД.

- [ ] StravaClient
    - getActivities(Long after, int page) — GET /athlete/activities
    - Пагинация по 50, пока не придёт пустой список
    - При 429 — пауза и повтор
- [ ] ActivityService.upsertActivities() — маппинг DTO → Entity, upsert по strava_id
- [ ] StravaPoller
    - @Scheduled каждые 5 минут
    - Если last_sync_at == 0 — полный бэкфилл всей истории
    - Иначе — только активности после last_sync_at
    - Обновить sync_state после успеха
- [ ] Проверить: в БД появились все активности

---

### Фаза 4 — Stats API
**Цель:** REST эндпоинты с данными для графиков.

- [ ] StatsService
    - getWeeklyVolume(int weeks) — км по неделям
    - getMonthlyVolume(int months) — км по месяцам
    - getSummary() — total distance / time / elevation
    - getPaceTrend(String type, int limit) — динамика темпа
    - getActivityTypeBreakdown() — Run/Ride/Swim/Other в %
- [ ] DashboardController
    - GET /api/stats/weekly?weeks=12&type=Run
    - GET /api/stats/monthly?months=6
    - GET /api/stats/summary
    - GET /api/stats/pace?type=Run&limit=30
    - GET /api/stats/breakdown
    - GET /api/activities?page=0&size=20
- [ ] Проверить: Postman — все эндпоинты возвращают корректный JSON

---

### Фаза 5 — React дашборд
**Цель:** визуальный дашборд с графиками.

- [ ] npm create vite@latest frontend + recharts, axios, date-fns, tailwindcss
- [ ] Компоненты:
    - SummaryCards — карточки: км / часы / набор высоты
    - WeeklyVolumeChart — bar chart по неделям
    - PaceChart — line chart динамики темпа
    - ActivityCalendar — heatmap активности
    - ActivityTypeBreakdown — donut chart
    - RecentActivitiesList — таблица последних активностей
- [ ] Автообновление каждые 60 секунд
- [ ] Проверить: дашборд открывается, данные отображаются

---

### Фаза 6 — Финализация
- [ ] CORS настройки в Spring
- [ ] Защита: редирект на /auth/strava если токена нет
- [ ] GET /api/sync/status — статус последней синхронизации
- [ ] POST /api/sync/trigger — кнопка "синхронизировать сейчас"
- [ ] README с инструкцией по запуску

---

## Константы Strava API

Base URL:        https://www.strava.com/api/v3
OAuth token URL: https://www.strava.com/oauth/token
Auth URL:        https://www.strava.com/oauth/authorize
Scope:           activity:read_all
Rate limit:      100 запросов / 15 мин, 1000 / день
Pagination:      per_page=50 (макс 200)
After param:     unix epoch timestamp

---

## Правила работы

1. Фаза за фазой — не переходим к следующей пока не работает текущая
2. Сначала пишем код, потом проверяем
3. Непонятно — спрашиваем до реализации, не после

---

Статус: Фаза 1 — не начата