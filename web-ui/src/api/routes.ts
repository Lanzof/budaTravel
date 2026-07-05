export type RouteOptimization = 'FASTEST' | 'CHEAPEST'
export type TransportType = 'BUS' | 'TRAIN' | 'METRO'

export interface RouteSearchRequest {
  originStopId: string
  destinationStopId: string
  departureDateTime: string
  optimization: RouteOptimization
  transportTypes: TransportType[]
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
}

export interface RouteResponseDto {
  totalDuration: string
  totalPrice: number
  segments: RouteSegmentDto[]
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
    throw new Error(`Route search failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<RouteResponseDto[]>
}
