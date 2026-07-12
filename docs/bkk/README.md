# BKK data/API reference

This folder keeps local reference material for BKK data sources used or considered by the project.

## Static GTFS archive

Primary source for the application's own imported transport graph.

```text
https://go.bkk.hu/api/static/v1/public-gtfs/budapest_gtfs.zip
```

Observed on 2026-07-12:

- response type: `application/zip`;
- full archive, not an incremental delta;
- size: about 48 MB (`Content-Length: 47927242` at the time of research);
- headers include `ETag`, `Last-Modified`, `Content-Length`, and `Cache-Control: max-age=86400`;
- conditional requests with `If-None-Match` / `If-Modified-Since` return `304 Not Modified` when unchanged;
- no API key was required for the public static archive endpoint during the check.

Planned project use:

- `demo` mode keeps using the tiny bundled archive for quick startup and CI;
- `bkk-static` mode downloads/checks the full public archive;
- the archive is cached outside git under `.local/gtfs-cache` through a Docker bind mount;
- DB rebuild for an updated full archive is intentionally not automatic until import generations/staging are designed.

## FUTÁR JSON API

Official OpenAPI file copied from BKK Swagger UI:

```text
docs/bkk/openapi/futar-openapi.yaml
```

The OpenAPI server is:

```text
https://futar.bkk.hu/api/query/v1/ws
```

Security scheme:

```text
key=<api-key>
```

The usual dialect path variable is:

```text
otp
```

Therefore a typical endpoint is:

```text
https://futar.bkk.hu/api/query/v1/ws/otp/api/where/search?version=2&appVersion=...&query=...&key=...
```

The API version enum in the OpenAPI file is `2`, `3`, `4`, `5`; the default is `2`.

Useful endpoints from the OpenAPI spec:

| Endpoint | Purpose |
| --- | --- |
| `/otp/api/where/search` | Search stops, routes, and alerts. |
| `/otp/api/where/stops-for-location` | Find stops around coordinates. |
| `/otp/api/where/arrivals-and-departures-for-stop` | Live-ish arrivals/departures for a stop. |
| `/otp/api/where/arrivals-and-departures-for-location` | Arrivals/departures around a map area. |
| `/otp/api/where/schedule-for-stop` | Stop schedule for a date. |
| `/otp/api/where/route-details` | Route variants, stops, references, and polyline. |
| `/otp/api/where/route-details-for-stop` | Route details constrained by a stop. |
| `/otp/api/where/trip-details` | Trip stop times, vehicle, and polyline. |
| `/otp/api/where/vehicle-for-trip` | Vehicle for a trip. |
| `/otp/api/where/vehicles` | Vehicle lookup. |
| `/otp/api/where/vehicles-for-location` | Vehicles around coordinates. |
| `/otp/api/where/vehicles-for-route` | Vehicles for a route. |
| `/otp/api/where/vehicles-for-stop` | Vehicles related to a stop. |
| `/otp/api/where/alert-search` | Search active alerts. |
| `/otp/api/where/alert-details` | Alert details. |
| `/otp/api/where/plan-trip` | BKK/FUTÁR route planner. Useful as a benchmark, not as a replacement for our own route logic. |
| `/otp/api/where/plan-access` | Accessibility/access planning. |
| `/otp/api/where/references` | ID-based references. |
| `/otp/api/where/metadata` | API metadata. |
| `/otp/api/where/ticketing-locations` | Ticketing-related locations. |
| `/otp/api/where/bicycle-rental` | Bicycle rental stations. |

Project stance:

- FUTÁR JSON is useful as a live enrichment/reference layer.
- It should not replace the project's own static GTFS import and routing model, otherwise the project becomes a thin BKK client.
- Good first uses: stop departures, live vehicles, alerts, BKK route planner benchmark.

## GTFS-Realtime protobuf feeds

Reference proto files are stored in:

```text
docs/bkk/proto/gtfs-realtime.proto
docs/bkk/proto/gtfs-realtime-realcity.proto
```

Feeds observed during research:

```text
https://go.bkk.hu/api/query/v1/ws/gtfs-rt/full/VehiclePositions.pb
https://go.bkk.hu/api/query/v1/ws/gtfs-rt/full/TripUpdates.pb
https://go.bkk.hu/api/query/v1/ws/gtfs-rt/full/Alerts.pb
```

The `.txt` variants are useful for manual inspection:

```text
VehiclePositions.txt
TripUpdates.txt
Alerts.txt
```

Observed on 2026-07-12 using the provided API key:

- `VehiclePositions`: about 866 vehicle entities;
- `TripUpdates`: about 3676 trip update entities plus a few `shape` entities;
- `Alerts`: about 64 alert entities;
- realtime feeds use `Cache-Control: no-cache, no-store` and `Last-Modified`;
- `TripUpdates.txt` can be very large, so `.txt` should be treated as debug-only.

Project stance:

- GTFS-RT is promising for the educational/realtime part of the project.
- It introduces protobuf, standard realtime feeds, and BKK/realCity extensions.
- It should be a separate future story after the static import model is clean.
