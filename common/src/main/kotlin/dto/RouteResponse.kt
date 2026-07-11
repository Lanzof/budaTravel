package io.lanzof.dto

import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime

data class RouteResponse(
    val totalDuration: Duration,
    val totalPrice: BigDecimal,
    val segments: List<RouteSegment>
)

data class RouteSegment(
    val from: RouteStop,
    val to: RouteStop,
    val timing: RouteSegmentTiming,
    val transport: RouteTransport,
    val gtfs: GtfsSegmentMetadata? = null,
    val geometry: List<RouteGeometryPoint> = emptyList(),
)

data class RouteStop(
    val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)

data class RouteSegmentTiming(
    val departureTime: OffsetDateTime,
    val arrivalTime: OffsetDateTime,
)

data class RouteTransport(
    val carrier: String,
    val type: TransportType,
    val routeId: String? = null,
)

data class GtfsSegmentMetadata(
    val tripId: String? = null,
    val shapeId: String? = null,
    val fromStopSequence: Int? = null,
    val toStopSequence: Int? = null,
)

data class RouteGeometryPoint(
    val lat: Double,
    val lon: Double,
)
