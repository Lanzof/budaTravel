## Спецификация Routing API (budaTravel)

### 1. Обзор

Документ описывает HTTP-эндпоинты для:

- управления и поиска **локаций** (остановок);
- **мультимаршрутного** поиска пути между остановками с оптимизацией по **времени** или **стоимости**.

Текущий стек:

- Backend: Kotlin, Spring Boot.
- Модули:
  - `core` — доменные сущности и репозитории (`Location`, `Connection`, `LocationRepo`, `ConnectionRepo`, `RouteService`);
  - `api` — HTTP API и DTO;
  - `ingestor` — загрузка GTFS в БД;
  - `common` — общие DTO (`RouteResponse` и др.).

API переиспользует существующую доменную модель там, где это возможно.  
Жизненный цикл схемы БД управляется Flyway; JPA `ddl-auto` работает в режиме `validate`.

---

### 2. Доменная модель и данные

#### 2.1. Location

- Сущность: `io.lanzof.core.entity.Location`
- Поля (для API):
  - `stopId: String` — внешний идентификатор остановки (из GTFS);
  - `name: String` — отображаемое имя остановки;
  - `lat: Double` — широта;
  - `lon: Double` — долгота;
  - внутренний `id: UUID` **не отдаётся** клиентам.

#### 2.2. Connection

- Сущность: `io.lanzof.core.entity.Connection`
- Поля (для маршрутизации):
  - `fromLocation: Location`
  - `toLocation: Location`
  - `departureTime: OffsetDateTime`
  - `arrivalTime: OffsetDateTime`
  - `price: BigDecimal`
  - `carrier: String`
  - `type: String` — тип транспорта:
    - минимум поддерживаются `"BUS"`, `"TRAIN"`, `"METRO"`;
    - в текущих GTFS-данных Будапешта фактически используется `"BUS"`;
    - значение `"PUBLIC_TRANSPORT"` в данных обрабатывается как алиас `"BUS"`.

#### 2.3. DTO маршрутов

Используются следующие DTO:

- `RouteResponse` (целый маршрут):
  - `totalDuration: string` — длительность в ISO-8601, например `"PT1H30M"`;
  - `totalPrice: number` — суммарная стоимость;
  - `segments: RouteSegment[]` — упорядоченный список сегментов от точки отправления до назначения.

- `RouteSegment`:
  - `fromStopId: string`
  - `toStopId: string`
  - `fromName: string`
  - `toName: string`
  - `fromLat: number`
  - `fromLon: number`
  - `toLat: number`
  - `toLon: number`
  - `departureTime: string` — ISO-8601 `OffsetDateTime`, например `"2025-01-10T14:30:00+01:00"`;
  - `arrivalTime: string` — ISO-8601 `OffsetDateTime`;
  - `carrier: string`
  - `type: string` — `"BUS"` / `"TRAIN"` / `"METRO"` / ...

Все timestamps в ответах должны содержать timezone offset.  
Формат `Z` (UTC offset) также является валидным ISO-8601 offset представлением.

---

### 3. API локаций

Базовый путь: `/api/v1/locations`  
Все ответы в формате JSON.

#### 3.1. GET /api/v1/locations

**Назначение:** список остановок с фильтрами и пагинацией.

**Параметры запроса:**

- `q: string` (optional) — поиск подстроки в названии, без учёта регистра;
- `stopId: string` (optional) — точное совпадение по `stopId`;
- `limit: integer` (optional, default = 50, max = 200);
- `offset: integer` (optional, default = 0).

**Правило взаимоисключения:**

- если переданы одновременно `q` и `stopId`, API возвращает `400 Bad Request`.

**Ответ 200 OK**

Тело: `LocationDto[]`

```json
[
  {
    "stopId": "ABC123",
    "name": "Budapest, Example Stop",
    "lat": 47.4979,
    "lon": 19.0402
  }
]
```

`LocationDto`:

- `stopId: string`
- `name: string`
- `lat: number`
- `lon: number`

**Ответ 400 Bad Request**

- при одновременной передаче `q` и `stopId`;
- при невалидных `limit`/`offset` (отрицательные, нечисловые и т.д.).

Пример:

```json
{
  "code": "INVALID_QUERY_PARAMETERS",
  "message": "Parameters 'q' and 'stopId' cannot be used together."
}
```

---

#### 3.2. GET /api/v1/locations/{stopId}

**Назначение:** получить одну локацию по `stopId`.

**Path-параметр:**

- `stopId: string` — внешний идентификатор остановки.

**Ответ 200 OK**

```json
{
  "stopId": "ABC123",
  "name": "Budapest, Example Stop",
  "lat": 47.4979,
  "lon": 19.0402
}
```

**Ответ 404 Not Found**

- если остановка с таким `stopId` не найдена.

---

#### 3.3. GET /api/v1/locations/autocomplete

**Назначение:** облегчённый endpoint для подсказок в UI.

**Параметры запроса:**

- `q: string` (required) — строка для поиска подсказок;
- `limit: integer` (optional, default = 10, max = 50).

**Ответ 200 OK**

Тело: `LocationSuggestionDto[]` (те же поля, что у `LocationDto`).

```json
[
  {
    "stopId": "ABC123",
    "name": "Budapest, Example Stop",
    "lat": 47.4979,
    "lon": 19.0402
  }
]
```

**Ответ 400 Bad Request**

- если `q` отсутствует или пустой.

---

### 4. API поиска маршрута (multi-hop)

Базовый путь: `/api/v1/routes`.

`POST /api/v1/routes/search` переопределён под multi-hop маршрутизацию с оптимизацией по скорости/стоимости, при этом формат ответа остаётся `RouteResponse[]`.

#### 4.1. Бизнес-правила

- Поддерживается поиск пути через несколько пересадок (A → ... → B) по графу `Connection`.
- Оптимизация по умолчанию: **FASTEST**.
- Альтернатива: **CHEAPEST**.
- Максимум пересадок — **3** (то есть максимум 4 сегмента).
- Время:
  - хранение/обработка в `OffsetDateTime`;
  - клиент передаёт `departureDateTime` в ISO-8601 с offset;
  - отдельного поля `timeZone` нет.
- Нормализация длительности сегмента:
  - если `arrivalTime == departureTime` (0 секунд), длительность сегмента считается **1 минута**;
  - `totalDuration` маршрута считается с учётом этой нормализации.
- Ограничения алгоритма (гарантии реализации):
  - используется best-first поиск (Dijkstra-like), а не полный перебор всех путей;
  - максимум возвращаемых маршрутов на запрос: **5**;
  - полная выгрузка таблицы `connections` в память не используется.

#### 4.2. Запрос: POST /api/v1/routes/search

**Тело: `RouteSearchRequest`**

```json
{
  "originStopId": "ORIGIN_STOP",
  "destinationStopId": "DEST_STOP",
  "departureDateTime": "2025-01-10T14:30:00+01:00",
  "optimization": "FASTEST",
  "transportTypes": ["BUS"]
}
```

Поля:

- `originStopId: string` (required)
- `destinationStopId: string` (required)
- `departureDateTime: string` (required)
  - ISO-8601 с offset, например `"2025-01-10T14:30:00+01:00"`;
  - сервер валидирует формат и использует распарсенный `OffsetDateTime` в запросах маршрутизации.
- `optimization: string` (optional)
  - значения: `"FASTEST"`, `"CHEAPEST"`;
  - default: `"FASTEST"`.
- `transportTypes: string[]` (optional)
  - допустимые значения (расширяемо): `"BUS"`, `"TRAIN"`, `"METRO"`, ...;
  - для текущего GTFS фактически доступен только `"BUS"`;
  - default: `["BUS"]`.

Потенциально в будущем:

- `maxResults` (optional) — ограничение количества маршрутов.

> Правило "максимум 3 пересадки" должно соблюдаться всегда.

---

#### 4.3. Ответ: 200 OK

**Тело: `RouteResponse[]`**

```json
[
  {
    "totalDuration": "PT1H30M",
    "totalPrice": 0.0,
    "segments": [
      {
        "fromStopId": "STOP_A",
        "toStopId": "STOP_B",
        "fromName": "Budapest, Stop A",
        "toName": "Budapest, Stop B",
        "fromLat": 47.4979,
        "fromLon": 19.0402,
        "toLat": 47.4985,
        "toLon": 19.0450,
        "departureTime": "2025-01-10T14:30:00+01:00",
        "arrivalTime": "2025-01-10T14:50:00+01:00",
        "carrier": "BKK",
        "type": "BUS"
      },
      {
        "fromStopId": "STOP_B",
        "toStopId": "STOP_C",
        "fromName": "Budapest, Stop B",
        "toName": "Budapest, Stop C",
        "fromLat": 47.4985,
        "fromLon": 19.0450,
        "toLat": 47.5010,
        "toLon": 19.0500,
        "departureTime": "2025-01-10T15:00:00+01:00",
        "arrivalTime": "2025-01-10T15:30:00+01:00",
        "carrier": "BKK",
        "type": "BUS"
      }
    ]
  }
]
```

Каждый `RouteResponse`:

- представляет полный путь от `originStopId` до `destinationStopId`;
- содержит `segments[]` в фактическом порядке поездки;
- число пересадок = `segments.size - 1` и должно быть ≤ 3.

---

#### 4.4. Обработка ошибок

- **400 Bad Request**
  - отсутствуют/некорректны поля `RouteSearchRequest`:
    - нет `originStopId`/`destinationStopId`;
    - нет `departureDateTime` или неверный формат;
    - `originStopId == destinationStopId`;
    - неподдерживаемый `optimization`;
    - неподдерживаемые значения в `transportTypes`.

Пример:

```json
{
  "code": "INVALID_REQUEST",
  "message": "Field 'departureDateTime' must be a valid ISO-8601 datetime with timezone offset."
}
```

- **404 Not Found**
  - `originStopId` или `destinationStopId` отсутствует в таблице локаций.

- **422 Unprocessable Entity**
  - подходящий маршрут не найден с учётом:
    - origin;
    - destination;
    - departureDateTime;
    - max 3 transfers;
    - выбранных `transportTypes`.

Пример:

```json
{
  "code": "NO_ROUTE_FOUND",
  "message": "No route found for the given parameters."
}
```

---

### 5. Интеграция с картой

Зона ответственности backend:

- отдавать координаты для:
  - локаций (`/locations`, `/locations/autocomplete`);
  - сегментов маршрута (`RouteSegment.fromLat/fromLon/toLat/toLon`).

Зона ответственности frontend:

- использовать MapLibre/Google Maps для:
  - маркеров остановок;
  - полилиний между сегментами.

Backend не генерирует подробную дорожную геометрию, только координаты stop-to-stop.

---

### 6. OpenAPI / Документация

OpenAPI генерируется SpringDoc + аннотациями контроллеров и включает:

- endpoints:
  - `GET /api/v1/locations`
  - `GET /api/v1/locations/{stopId}`
  - `GET /api/v1/locations/autocomplete`
  - `POST /api/v1/routes/search`
- схемы:
  - `LocationDto`
  - `LocationSuggestionDto`
  - `RouteSearchRequest`
  - `RouteResponse`
  - `RouteSegment`
  - модель ошибок.

Swagger UI должен явно отражать:

- формат времени (ISO-8601 с offset);
- варианты оптимизации;
- ограничение в 3 пересадки.

---

### 7. Ограничения хранения и запросов

- Схема и индексы БД управляются миграциями Flyway:
  - `V1__init_schema.sql` — базовые таблицы/ограничения;
  - `V2__routing_indexes.sql` — индексы для маршрутизации.
- Критичные индексы:
  - уникальный `location(stop_id)`;
  - `connections(from_location_id, departure_time)`;
  - `connections(type, departure_time)`.
- Расширение маршрута идёт через ограниченный next-hop запрос:
  - `from_location_id`,
  - `departure_time >= :time`,
  - `type IN (...)`,
  - сортировка по `departure_time`.
- Автогенерация схемы рантаймом не является основным механизмом.

---

### 8. Критерии приёмки

1. **Location API**
   - `GET /api/v1/locations`:
     - работает с `q`, `stopId`, пагинацией;
     - возвращает 400 при одновременном `q` и `stopId`.
   - `GET /api/v1/locations/{stopId}`:
     - 200 для существующего `stopId`;
     - 404 для отсутствующего `stopId`.
   - `GET /api/v1/locations/autocomplete`:
     - требует `q`, возвращает ограниченные подсказки.

2. **Route Search API**
   - `POST /api/v1/routes/search`:
     - принимает `RouteSearchRequest` в описанном формате;
     - поддерживает `optimization = FASTEST | CHEAPEST`;
     - соблюдает ограничение максимум 3 пересадки;
     - применяет правило нормализации `0 seconds => 1 minute`;
     - возвращает `RouteResponse[]` с координатами сегментов;
     - валидирует `departureDateTime` как ISO-8601 с offset;
     - возвращает максимум 5 лучших маршрутов, отсортированных по выбранной оптимизации.

3. **Ошибки**
   - структурированные и осмысленные ответы 400/404/422.

4. **Документация**
   - OpenAPI/Swagger отражает актуальные контракты;
   - примеры запросов/ответов доступны через Swagger UI.

5. **Ограничения производительности**
   - в маршрутизации нет `connectionRepo.findAll()`/полной выгрузки таблицы;
   - индексы маршрутизации поставляются через Flyway-миграции.
