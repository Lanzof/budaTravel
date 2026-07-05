import { useEffect, useMemo, useState } from 'react'
import { CircleMarker, MapContainer, Popup, TileLayer, useMapEvents } from 'react-leaflet'
import type { LatLngBounds } from 'leaflet'
import { fetchLocations, type BoundingBox, type LocationDto } from '../api/locations'
import 'leaflet/dist/leaflet.css'

const BUDAPEST_CENTER: [number, number] = [47.4979, 19.0402]
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

export function StopsMap() {
  const [bounds, setBounds] = useState<BoundingBox>(DEFAULT_BOUNDS)
  const [locations, setLocations] = useState<LocationDto[]>([])
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

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

  return (
    <section className="map-shell" aria-label="Budapest stops map">
      <div className="map-panel">
        <MapContainer center={BUDAPEST_CENTER} zoom={13} className="map" scrollWheelZoom>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapBoundsWatcher onBoundsChange={setBounds} />
          {locations.map((location) => (
            <CircleMarker
              key={location.stopId}
              center={[location.lat, location.lon]}
              radius={6}
              pathOptions={{
                color: selectedStopId === location.stopId ? '#f97316' : '#2563eb',
                fillColor: selectedStopId === location.stopId ? '#fdba74' : '#60a5fa',
                fillOpacity: 0.85,
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
              </Popup>
            </CircleMarker>
          ))}
        </MapContainer>
      </div>

      <aside className="sidebar" aria-label="Loaded stops summary">
        <div>
          <p className="eyebrow">budaTravel MVP</p>
          <h2>Stops in current map view</h2>
          <p className="muted">
            The UI calls <code>/api/v1/locations</code> with the visible map bounding box and renders the
            returned stops as Leaflet points.
          </p>
        </div>

        <dl className="stats-grid">
          <div>
            <dt>Loaded stops</dt>
            <dd>{locations.length}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{isLoading ? 'Loading' : 'Idle'}</dd>
          </div>
        </dl>

        {error ? (
          <div className="notice error" role="alert">
            Backend is not reachable yet: {error}. Start the API locally and keep this page open.
          </div>
        ) : null}

        {selectedLocation ? (
          <div className="selected-card">
            <p className="eyebrow">Selected stop</p>
            <h3>{selectedLocation.name}</h3>
            <p>{selectedLocation.stopId}</p>
            <p>
              {selectedLocation.lat.toFixed(6)}, {selectedLocation.lon.toFixed(6)}
            </p>
          </div>
        ) : (
          <div className="notice">Click a stop point to inspect its id and coordinates.</div>
        )}
      </aside>
    </section>
  )
}
