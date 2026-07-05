# budaTravel web UI

Minimal React/Vite/TypeScript UI for the budaTravel MVP.

## Stack

- React
- Vite
- TypeScript
- Leaflet / React Leaflet
- OpenStreetMap-compatible map tiles

## Local development

Start the backend API from the repository root:

```bash
./gradlew :api:bootRun
```

Start the frontend:

```bash
cd web-ui
npm install
npm run dev
```

Open the Vite URL shown in the terminal, usually <http://localhost:5173>.

The UI requests stops from:

```text
GET /api/v1/locations?minLat=...&maxLat=...&minLon=...&maxLon=...&limit=200
```

It can also search the fastest bus route between two selected stops. For the current demo data, route search uses the stable GTFS service timestamp `2026-01-27T04:44:00+01:00`:

```text
POST /api/v1/routes/search
```

During local development, Vite proxies `/api` requests to `http://localhost:8080`.
