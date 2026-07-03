package io.lanzof.core.service

import io.lanzof.dto.RouteOptimization
import io.lanzof.dto.RouteResponse
import io.lanzof.dto.TransportType
import java.time.OffsetDateTime

interface RoutingService {
    fun findRoutes(
        originStopId: String,
        destinationStopId: String,
        departureDateTime: OffsetDateTime,
        optimization: RouteOptimization = RouteOptimization.FASTEST,
        transportTypes: Set<TransportType> = setOf(TransportType.BUS),
    ): List<RoutingPath>

    fun toRouteResponses(paths: List<RoutingPath>): List<RouteResponse>
}

