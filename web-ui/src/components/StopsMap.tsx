import { useEffect, useMemo, useState } from 'react'
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMapEvents } from 'react-leaflet'
import type { LatLngBounds, LatLngExpression } from 'leaflet'
import { fetchLocations, type BoundingBox, type LocationDto } from '../api/locations'
import { RouteSearchError, searchRoutes, type RouteResponseDto } from '../api/routes'
import 'leaflet/dist/leaflet.css'

const BUDAPEST_CENTER: [number, number] = [47.4979, 19.0402]
const DEMO_DEPARTURE_DATETIME = '2026-01-27T04:44:00+01:00'
const DEMO_ORIGIN: LocationDto = {
  stopId: 'F00985',
  name: 'Deak Ferenc ter M',
  lat: 47.497701,
  lon: 19.053353,
}
const DEMO_DESTINATION: LocationDto = {
  stopId: 'F00045',
  name: 'Donati utca',
  lat: 47.501307,
  lon: 19.036072,
}

const DEFAULT_BOUNDS: BoundingBox = {
  minLat: 47.45,
  maxLat: 47.55,
  minLon: 18.95,
  maxLon: 19.15,
}

interface MapBoundsWatcherProps {
  onBoundsChange: (bounds: BoundingBox) => void
}

function toBoundingBox(bounds: LatLngBounds): BoundingBox {
  const southWest = bounds.getSouthWest()
  const northEast = bounds.getNorthEast()

  return {
    minLat: southWest.lat,
    maxLat: northEast.lat,
    minLon: southWest.lng,
    maxLon: northEast.lng,
  }
}

function MapBoundsWatcher({ onBoundsChange }: MapBoundsWatcherProps): null {
  const map = useMapEvents({
    load: () => onBoundsChange(toBoundingBox(map.getBounds())),
    moveend: () => onBoundsChange(toBoundingBox(map.getBounds())),
    zoomend: () => onBoundsChange(toBoundingBox(map.getBounds())),
  })

  useEffect(() => {
    onBoundsChange(toBoundingBox(map.getBounds()))
  }, [map, onBoundsChange])

  return null
}

function routeToPolyline(route: RouteResponseDto | null): LatLngExpression[] {
  if (!route) {
    return []
  }

  return route.segments.flatMap((segment) => {
    if (segment.geometry && segment.geometry.length > 0) {
      return segment.geometry.map((point) => [point.lat, point.lon] as LatLngExpression)
    }

    return [
      [segment.from.lat, segment.from.lon] as LatLngExpression,
      [segment.to.lat, segment.to.lon] as LatLngExpression,
    ]
  })
}

function formatDuration(duration: string): string {
  return duration.replace('PT', '').replace('H', 'h ').replace('M', 'm').replace('S', 's')
}

function formatTime(value: string): string {
  const timeMatch = /T(?<time>\d{2}:\d{2})/.exec(value)
  return timeMatch?.groups?.time ?? value
}

function formatSegmentDuration(departureTime: string, arrivalTime: string): string {
  const durationMs = new Date(arrivalTime).getTime() - new Date(departureTime).getTime()
  if (!Number.isFinite(durationMs) || durationMs <= 0) {
    return '≈1m'
  }

  const totalMinutes = Math.max(1, Math.round(durationMs / 60_000))
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
}

function formatStopsCount(fromSequence?: number | null, toSequence?: number | null): string | null {
  if (fromSequence == null || toSequence == null) {
    return null
  }

  const stops = Math.abs(toSequence - fromSequence)
  return `${stops} stop${stops === 1 ? '' : 's'}`
}

function toRouteErrorMessage(error: unknown): string {
  if (error instanceof RouteSearchError && error.status === 422) {
    return 'Route not found in the bundled mini demo dataset. Try the demo route: Deak Ferenc ter M → Donati utca.'
  }

  if (error instanceof RouteSearchError) {
    return error.message
  }

  return error instanceof Error ? error.message : 'Unknown route search error'
}

export function StopsMap() {
  const [bounds, setBounds] = useState<BoundingBox>(DEFAULT_BOUNDS)
  const [locations, setLocations] = useState<LocationDto[]>([])
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null)
  const [origin, setOrigin] = useState<LocationDto | null>(null)
  const [destination, setDestination] = useState<LocationDto | null>(null)
  const [route, setRoute] = useState<RouteResponseDto | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isSearchingRoute, setIsSearchingRoute] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [routeError, setRouteError] = useState<string | null>(null)

  useEffect(() => {
    const abortController = new AbortController()

    async function loadLocations(): Promise<void> {
      setIsLoading(true)
      setError(null)

      try {
        const nextLocations = await fetchLocations(bounds, abortController.signal)
        setLocations(nextLocations)
      } catch (nextError) {
        if (abortController.signal.aborted) {
          return
        }
        setError(nextError instanceof Error ? nextError.message : 'Unknown locations loading error')
      } finally {
        if (!abortController.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadLocations()

    return () => abortController.abort()
  }, [bounds])

  const selectedLocation = useMemo(
    () => locations.find((location) => location.stopId === selectedStopId) ?? null,
    [locations, selectedStopId],
  )

  const routeLine = useMemo(() => routeToPolyline(route), [route])
  const canSearchRoute = origin !== null && destination !== null && origin.stopId !== destination.stopId

  async function handleRouteSearch(): Promise<void> {
    if (!origin || !destination || origin.stopId === destination.stopId) {
      return
    }

    setIsSearchingRoute(true)
    setRouteError(null)
    setRoute(null)

    try {
      const routes = await searchRoutes({
        originStopId: origin.stopId,
        destinationStopId: destination.stopId,
        departureDateTime: DEMO_DEPARTURE_DATETIME,
        optimization: 'FASTEST',
        transportTypes: ['BUS'],
      })
      setRoute(routes[0] ?? null)
      if (routes.length === 0) {
        setRouteError('No routes returned by backend.')
      }
    } catch (nextError) {
      setRouteError(toRouteErrorMessage(nextError))
    } finally {
      setIsSearchingRoute(false)
    }
  }

  function markAsOrigin(location: LocationDto): void {
    setOrigin(location)
    setRoute(null)
    setRouteError(null)
  }

  function markAsDestination(location: LocationDto): void {
    setDestination(location)
    setRoute(null)
    setRouteError(null)
  }

  function useDemoRoute(): void {
    setOrigin(DEMO_ORIGIN)
    setDestination(DEMO_DESTINATION)
    setSelectedStopId(DEMO_ORIGIN.stopId)
    setRoute(null)
    setRouteError(null)
  }

  return (
    <>
      <header className="hero-header">
        <div className="hero-copy">
          <p className="eyebrow">Budapest transport demo</p>
          <h1>budaTravel map MVP</h1>
          <p>
            Minimal web UI for exploring stops imported from the backend demo dataset. Move the map to request a fresh
            bounding box from the API.
          </p>
        </div>

        <section className="hero-status-panel" aria-label="Demo status and shortcuts">
          <div className="hero-endpoint-note">
            <p className="eyebrow">Debug panel</p>
            <strong>Demo runtime status</strong>
          </div>

          <dl className="stats-grid hero-stats">
            <div>
              <dt>Loaded stops</dt>
              <dd>{locations.length}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{isLoading ? 'Loading' : 'Idle'}</dd>
            </div>
          </dl>

          <button type="button" className="secondary-action demo-action" onClick={useDemoRoute}>
            Use demo route
          </button>

          {error ? (
            <div className="notice error hero-error" role="alert">
              Backend is not reachable yet: {error}. Start the API locally and keep this page open.
            </div>
          ) : null}
        </section>
      </header>

      <section className="map-shell" aria-label="Budapest stops map">
        <div className="map-panel">
          <MapContainer center={BUDAPEST_CENTER} zoom={13} className="map" scrollWheelZoom>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <MapBoundsWatcher onBoundsChange={setBounds} />
            {routeLine.length > 0 ? (
              <Polyline positions={routeLine} pathOptions={{ color: '#f97316', weight: 5, opacity: 0.85 }} />
            ) : null}
            {locations.map((location) => {
              const isOrigin = origin?.stopId === location.stopId
              const isDestination = destination?.stopId === location.stopId
              const isSelected = selectedStopId === location.stopId

              return (
                <CircleMarker
                  key={location.stopId}
                  center={[location.lat, location.lon]}
                  radius={isOrigin || isDestination ? 8 : 6}
                  pathOptions={{
                    color: isOrigin ? '#16a34a' : isDestination ? '#dc2626' : isSelected ? '#f97316' : '#2563eb',
                    fillColor: isOrigin
                      ? '#86efac'
                      : isDestination
                        ? '#fca5a5'
                        : isSelected
                          ? '#fdba74'
                          : '#60a5fa',
                    fillOpacity: 0.88,
                    weight: 2,
                  }}
                  eventHandlers={{ click: () => setSelectedStopId(location.stopId) }}
                >
                  <Popup>
                    <strong>{location.name}</strong>
                    <br />
                    <span>{location.stopId}</span>
                    <br />
                    <span>
                      {location.lat.toFixed(5)}, {location.lon.toFixed(5)}
                    </span>
                    <div className="popup-actions">
                      <button type="button" onClick={() => markAsOrigin(location)}>
                        Set origin
                      </button>
                      <button type="button" onClick={() => markAsDestination(location)}>
                        Set destination
                      </button>
                    </div>
                  </Popup>
                </CircleMarker>
              )
            })}
          </MapContainer>
        </div>

        <aside className="sidebar" aria-label="Route planning panel">
          <div className="selected-card selected-card-placeholder">
            <p className="eyebrow">Selected stop</p>
            {selectedLocation ? (
              <>
                <h3>{selectedLocation.name}</h3>
                <p>{selectedLocation.stopId}</p>
                <p>
                  {selectedLocation.lat.toFixed(6)}, {selectedLocation.lon.toFixed(6)}
                </p>
                <div className="button-row">
                  <button type="button" onClick={() => markAsOrigin(selectedLocation)}>
                    Set as origin
                  </button>
                  <button type="button" onClick={() => markAsDestination(selectedLocation)}>
                    Set as destination
                  </button>
                </div>
              </>
            ) : (
              <div className="empty-stop-state">
                <h3>No stop selected</h3>
                <p>Click a stop point on the map to inspect it or set it as route origin/destination.</p>
              </div>
            )}
          </div>

          <div className="route-card">
            <p className="eyebrow">Route search</p>
            <div className="route-stop-row">
              <span>Origin</span>
              <strong>{origin?.name ?? 'Not selected'}</strong>
            </div>
            <div className="route-stop-row">
              <span>Destination</span>
              <strong>{destination?.name ?? 'Not selected'}</strong>
            </div>
            <p className="route-hint">
              Uses the demo GTFS service date: <code>2026-01-27 04:44 Europe/Budapest</code>.
            </p>
            <button type="button" className="primary-action" disabled={!canSearchRoute || isSearchingRoute} onClick={handleRouteSearch}>
              {isSearchingRoute ? 'Searching…' : 'Search fastest bus route'}
            </button>
            {routeError ? (
              <div className="notice error" role="alert">
                {routeError}
              </div>
            ) : null}
          </div>

          <div className="route-summary-card">
            <p className="eyebrow">Route summary</p>
            {route ? (
              <div className="route-result">
                <strong>{route.segments.length} segment(s)</strong>
                <span>{formatDuration(route.totalDuration)}</span>
                <span>Total price: {route.totalPrice}</span>
              </div>
            ) : (
              <p className="muted">Choose origin and destination, then run route search to see summary.</p>
            )}
          </div>

          <div className="route-segments-card">
            <p className="eyebrow">Segments</p>
            {route ? (
              <ol className="route-segments" aria-label="Route details">
                {route.segments.map((segment, index) => {
                  const stopsCount = formatStopsCount(segment.gtfs?.fromStopSequence, segment.gtfs?.toStopSequence)

                  return (
                    <li key={`${segment.from.stopId}-${segment.to.stopId}-${segment.timing.departureTime}`} className="route-segment">
                      <div className="route-segment-index">{index + 1}</div>
                      <div className="route-segment-body">
                        <div className="route-segment-main">
                          <strong>
                            {segment.from.name} → {segment.to.name}
                          </strong>
                          <span>
                            {formatTime(segment.timing.departureTime)}–{formatTime(segment.timing.arrivalTime)} ·{' '}
                            {formatSegmentDuration(segment.timing.departureTime, segment.timing.arrivalTime)}
                          </span>
                        </div>
                        <div className="route-segment-meta">
                          <span>{segment.transport.routeId ? `Route ${segment.transport.routeId}` : segment.transport.carrier}</span>
                          <span>{segment.transport.type}</span>
                          {stopsCount ? <span>{stopsCount}</span> : null}
                          {segment.geometry && segment.geometry.length > 0 ? <span>{segment.geometry.length} shape points</span> : null}
                        </div>
                        <p className="route-segment-debug">
                          {segment.from.stopId} → {segment.to.stopId}
                          {segment.gtfs?.tripId ? ` · trip ${segment.gtfs?.tripId}` : ''}
                          {segment.gtfs?.shapeId ? ` · shape ${segment.gtfs?.shapeId}` : ''}
                        </p>
                      </div>
                    </li>
                  )
                })}
              </ol>
            ) : (
              <p className="muted">Route segments will appear here after a successful search.</p>
            )}
          </div>
        </aside>
      </section>
    </>
  )
}
