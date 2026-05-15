# Strava Dashboard

Персональный дашборд для визуализации активностей Strava.

## Стек

- **Backend**: Java 21, Spring Boot 4, PostgreSQL, Flyway
- **Frontend**: React 19, Recharts, Tailwind CSS 4, Vite

## Требования

- Java 21+
- Docker (для PostgreSQL)
- Node.js 18+
- Аккаунт Strava + зарегистрированное приложение на [strava.com/settings/api](https://www.strava.com/settings/api)

## Запуск

### 1. База данных

```bash
docker compose up -d
```

### 2. Backend

Задать переменные окружения в Run Configuration (IntelliJ) или в терминале:

```bash
export STRAVA_CLIENT_ID=ваш_client_id
export STRAVA_CLIENT_SECRET=ваш_client_secret
./mvnw spring-boot:run
```

### 3. Авторизация Strava

Открыть в браузере: http://localhost:8080/auth/strava

После авторизации токен сохранится в БД. Синхронизация активностей запустится автоматически на следующем срабатывании джобы (в течение часа) либо немедленно через `POST /api/sync/full`.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Открыть: http://localhost:5173

## Настройки Strava API

При регистрации приложения на strava.com/settings/api указать:

- **Authorization Callback Domain**: `localhost`

## Переменные окружения

| Переменная | Описание |
|---|---|
| `STRAVA_CLIENT_ID` | Client ID приложения Strava |
| `STRAVA_CLIENT_SECRET` | Client Secret приложения Strava |
| `STRAVA_REDIRECT_URI` | URI редиректа после авторизации (по умолчанию `http://localhost/auth/callback`) |

## API

### Авторизация

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/auth/strava` | Редирект на страницу авторизации Strava |
| `GET` | `/auth/callback?code=...` | OAuth callback, сохраняет токен и запускает синк |
| `GET` | `/auth/success` | Страница подтверждения авторизации |

### Активности

| Метод | Путь | Параметры | Описание |
|---|---|---|---|
| `GET` | `/api/activities` | `page=0`, `size=20` | Список активностей с пагинацией |
| `GET` | `/api/athlete` | — | Имя и аватар атлета (из Strava) |

### Статистика

| Метод | Путь | Параметры | Описание |
|---|---|---|---|
| `GET` | `/api/stats/summary` | — | Суммарные показатели по всем активностям |
| `GET` | `/api/stats/weekly` | `weeks=12`, `type=` | Объём по неделям (км), фильтр по типу |
| `GET` | `/api/stats/monthly` | `months=6` | Объём по месяцам (км) |
| `GET` | `/api/stats/pace` | `type=Ride`, `limit=30` | Тренд скорости/темпа по активностям |
| `GET` | `/api/stats/breakdown` | — | Разбивка по типам активностей |

### Синхронизация

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/sync/status` | Статус последней синхронизации |
| `POST` | `/api/sync/full` | Полный пересинк всех активностей из Strava |

## Логика синхронизации активностей

Приложение загружает активности из Strava в два режима.

**Первый запуск (пустая БД)**

Если в базе ещё нет ни одной активности, джоба отправляет запрос к Strava с `after=0` — Strava возвращает все активности атлета. Это происходит автоматически при первом срабатывании джобы после авторизации.

**Инкрементальная синхронизация (каждый час)**

`StravaPoller` запускается раз в час (`fixedDelay = 3600s`). В качестве параметра `after` передаётся `start_date` самой последней тренировки в БД (`MAX(start_date)`). Strava возвращает только активности, начавшиеся позже этой даты. Загрузка идёт постранично (50 записей), дубликаты обрабатываются через upsert по `strava_id`.

**Тренировка с более ранней датой**

Если в Strava появилась тренировка с датой начала _раньше_ последней тренировки в БД (загружена задним числом, импортирована с GPS-трекера и т.п.) — автоматически она не подхватится, так как её `start_date` окажется меньше значения `after`. В этом случае нужно запустить полный пересинк вручную:

```bash
curl -X POST http://localhost:8080/api/sync/full
```

Полный пересинк использует `after=0` и загружает все активности заново, не удаляя существующие данные.

## Деплой на сервер

### 1. Настройка Strava API

На [strava.com/settings/api](https://www.strava.com/settings/api) указать продовый домен:

- **Authorization Callback Domain**: `yourdomain.com`

### 2. Переменные окружения

Создать `.env` рядом с `docker-compose.yml`:

```env
STRAVA_CLIENT_ID=ваш_client_id
STRAVA_CLIENT_SECRET=ваш_client_secret
STRAVA_REDIRECT_URI=https://yourdomain.com/auth/callback
```

### 3. docker-compose

Поменять порт фронтенда (порт 80 занят реверс-прокси):

```yaml
frontend:
  ports:
    - "8081:80"   # любой свободный порт
```


### 4. Запуск

```bash
git pull
docker compose up -d --build
```

После запуска авторизоваться: `https://yourdomain.com/auth/strava`
