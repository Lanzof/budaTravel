package io.lanzof.api.controller

import io.lanzof.api.dto.ApiErrorResponse
import io.lanzof.api.exception.InvalidRequestException
import io.lanzof.api.exception.NoRouteFoundException
import io.lanzof.core.service.RouteService
import io.lanzof.dto.RouteResponse
import io.lanzof.dto.RouteSearchRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "Routes", description = "Route search endpoints")
class RoutesController(
    private val routeService: RouteService
) {
    @PostMapping("/search")
    @Operation(summary = "Search routes between two stops")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Routes found"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Origin or destination not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "422",
                description = "No route found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
        ]
    )
    fun searchRoutes(@RequestBody request: RouteSearchRequest): List<RouteResponse> {
        if (request.originStopId == request.destinationStopId) {
            throw InvalidRequestException("originStopId and destinationStopId must be different.")
        }
        val routes = routeService.findRoutes(
            from = request.originStopId,
            to = request.destinationStopId,
            departureDateTime = request.departureDateTime,
            optimization = request.optimization,
            transportTypes = request.transportTypes,
        )
        if (routes.isEmpty()) {
            throw NoRouteFoundException()
        }
        return routes
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    fun health(): String {
        return "API is running"
    }
}
