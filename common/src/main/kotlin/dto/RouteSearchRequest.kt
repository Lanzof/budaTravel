package io.lanzof.dto

import java.time.OffsetDateTime

data class RouteSearchRequest(
    val originStopId: String,
    val destinationStopId: String,
    val departureDateTime: OffsetDateTime,
    val optimization: RouteOptimization = RouteOptimization.FASTEST,
    val transportTypes: List<TransportType> = listOf(TransportType.BUS),
)

enum class RouteOptimization {
    FASTEST,
    CHEAPEST,
}

enum class TransportType {
    BUS,
    TRAIN,
    METRO,
}

