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
    val fromStopId: String,
    val toStopId: String,
    val fromName: String,
    val toName: String,
    val fromLat: Double,
    val fromLon: Double,
    val toLat: Double,
    val toLon: Double,
    val departureTime: OffsetDateTime,
    val arrivalTime: OffsetDateTime,
    val carrier: String,
    val type: TransportType
)
