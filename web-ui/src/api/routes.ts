export type RouteOptimization = 'FASTEST' | 'CHEAPEST'
export type TransportType = 'BUS' | 'TRAIN' | 'METRO'

export interface RouteSearchRequest {
  originStopId: string
  destinationStopId: string
  departureDateTime: string
  optimization: RouteOptimization
  transportTypes: TransportType[]
}

export interface RouteGeometryPointDto {
  lat: number
  lon: number
}

export interface RouteSegmentDto {
  fromStopId: string
  toStopId: string
  fromName: string
  toName: string
  fromLat: number
  fromLon: number
  toLat: number
  toLon: number
  departureTime: string
  arrivalTime: string
  carrier: string
  type: TransportType
  routeId?: string | null
  tripId?: string | null
  shapeId?: string | null
  fromStopSequence?: number | null
  toStopSequence?: number | null
  geometry?: RouteGeometryPointDto[]
}

export interface RouteResponseDto {
  totalDuration: string
  totalPrice: number
  segments: RouteSegmentDto[]
}

interface ApiErrorResponse {
  code?: string
  message?: string
}

export class RouteSearchError extends Error {
  readonly status: number
  readonly code?: string

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'RouteSearchError'
    this.status = status
    this.code = code
  }
}

export async function searchRoutes(request: RouteSearchRequest): Promise<RouteResponseDto[]> {
  const response = await fetch('/api/v1/routes/search', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    let apiError: ApiErrorResponse | null = null
    try {
      apiError = (await response.json()) as ApiErrorResponse
    } catch {
      apiError = null
    }

    throw new RouteSearchError(
      apiError?.message ?? `Route search failed with HTTP ${response.status}`,
      response.status,
      apiError?.code,
    )
  }

  return response.json() as Promise<RouteResponseDto[]>
}
