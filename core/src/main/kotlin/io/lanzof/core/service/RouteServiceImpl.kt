package io.lanzof.core.service

import io.lanzof.dto.RouteOptimization
import io.lanzof.dto.RouteResponse
import io.lanzof.dto.TransportType
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class RouteServiceImpl(
    private val routingService: RoutingService,
) : RouteService {
    override fun findRoutes(
        from: String,
        to: String,
        departureDateTime: OffsetDateTime,
        optimization: RouteOptimization,
        transportTypes: List<TransportType>,
    ): List<RouteResponse> {
        val paths = routingService.findRoutes(
            originStopId = from,
            destinationStopId = to,
            departureDateTime = departureDateTime,
            optimization = optimization,
            transportTypes = transportTypes.toSet(),
        )
        return routingService.toRouteResponses(paths)
    }
}
