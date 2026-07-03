package io.lanzof.core.service

import io.lanzof.dto.RouteResponse
import io.lanzof.dto.RouteOptimization
import io.lanzof.dto.TransportType
import java.time.OffsetDateTime

interface RouteService {
    fun findRoutes(
        from: String,
        to: String,
        departureDateTime: OffsetDateTime,
        optimization: RouteOptimization = RouteOptimization.FASTEST,
        transportTypes: List<TransportType> = listOf(TransportType.BUS),
    ): List<RouteResponse>
}
