export interface LocationDto {
  stopId: string
  name: string
  lat: number
  lon: number
}

export interface BoundingBox {
  minLat: number
  maxLat: number
  minLon: number
  maxLon: number
}

export async function fetchLocations(bounds: BoundingBox, signal?: AbortSignal): Promise<LocationDto[]> {
  const params = new URLSearchParams({
    minLat: bounds.minLat.toFixed(6),
    maxLat: bounds.maxLat.toFixed(6),
    minLon: bounds.minLon.toFixed(6),
    maxLon: bounds.maxLon.toFixed(6),
    limit: '200',
  })

  const response = await fetch(`/api/v1/locations?${params.toString()}`, { signal })

  if (!response.ok) {
    throw new Error(`Locations request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<LocationDto[]>
}
