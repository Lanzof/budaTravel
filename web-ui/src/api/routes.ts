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

export interface RouteStopDto {
  stopId: string
  name: string
  lat: number
  lon: number
}

export interface RouteSegmentTimingDto {
  departureTime: string
  arrivalTime: string
}

export interface RouteTransportDto {
  carrier: string
  type: TransportType
  routeId?: string | null
}

export interface GtfsSegmentMetadataDto {
  tripId?: string | null
  shapeId?: string | null
  fromStopSequence?: number | null
  toStopSequence?: number | null
}

export interface RouteSegmentDto {
  from: RouteStopDto
  to: RouteStopDto
  timing: RouteSegmentTimingDto
  transport: RouteTransportDto
  gtfs?: GtfsSegmentMetadataDto | null
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
