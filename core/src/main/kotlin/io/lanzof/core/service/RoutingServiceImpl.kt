package io.lanzof.core.service

import io.lanzof.core.entity.Connection
import io.lanzof.core.repo.ConnectionRepo
import io.lanzof.core.repo.GtfsShapePointRepo
import io.lanzof.core.repo.LocationRepo
import io.lanzof.dto.RouteGeometryPoint
import io.lanzof.dto.RouteOptimization
import io.lanzof.dto.RouteResponse
import io.lanzof.dto.RouteSegment
import io.lanzof.dto.TransportType
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime
import java.util.PriorityQueue

@Service
class RoutingServiceImpl(
    private val locationRepo: LocationRepo,
    private val connectionRepo: ConnectionRepo,
    private val shapePointRepo: GtfsShapePointRepo,
) : RoutingService {
    override fun findRoutes(
        originStopId: String,
        destinationStopId: String,
        departureDateTime: OffsetDateTime,
        optimization: RouteOptimization,
        transportTypes: Set<TransportType>,
    ): List<RoutingPath> {
        val origin = locationRepo.findByStopId(originStopId).firstOrNull()
            ?: throw StopNotFoundException(originStopId)
        val destination = locationRepo.findByStopId(destinationStopId).firstOrNull()
            ?: throw StopNotFoundException(destinationStopId)

        val routeRequestTypes = normalizeRequestTypes(transportTypes)
        val queue = PriorityQueue<SearchState>(stateComparator(optimization))
        queue.add(
            SearchState(
                currentStopId = origin.stopId,
                currentLocationId = origin.id!!,
                currentTime = departureDateTime,
                segments = emptyList(),
                visitedStopIds = setOf(origin.stopId),
                totalDuration = Duration.ZERO,
                totalPrice = BigDecimal.ZERO,
            )
        )

        val bestByStopAndDepth = mutableMapOf<Pair<String, Int>, SearchCost>()
        val found = mutableListOf<RoutingPath>()

        while (queue.isNotEmpty() && found.size < MAX_RESULTS) {
            val state = queue.poll()
            if (state.currentStopId == destination.stopId && state.segments.isNotEmpty()) {
                found.add(
                    RoutingPath(
                        segments = state.segments,
                        requestedDepartureDateTime = departureDateTime,
                    )
                )
                continue
            }
            if (state.segments.size >= MAX_SEGMENTS) {
                continue
            }

            val key = state.currentStopId to state.segments.size
            val stateCost = SearchCost(state.totalDuration, state.totalPrice)
            val bestKnown = bestByStopAndDepth[key]
            if (bestKnown != null && costDominates(bestKnown, stateCost, optimization)) {
                continue
            }
            bestByStopAndDepth[key] = stateCost

            val nextConnections = connectionRepo.findNextConnections(
                fromId = state.currentLocationId,
                time = state.currentTime,
                types = routeRequestTypes,
                pageable = PageRequest.of(0, STEP_CANDIDATES_LIMIT),
            )

            for (connection in nextConnections) {
                if (connection.arrivalTime < connection.departureTime) {
                    continue
                }
                if (connection.toLocation.stopId in state.visitedStopIds) {
                    continue
                }
                val normalizedType = normalizeTransportType(connection.type) ?: continue
                if (normalizedType !in normalizeRequestTypeSet(transportTypes)) {
                    continue
                }

                val segmentDuration = RoutingPath.normalizedSegmentDuration(connection.departureTime, connection.arrivalTime)
                val nextState = SearchState(
                    currentStopId = connection.toLocation.stopId,
                    currentLocationId = connection.toLocation.id
                        ?: continue,
                    currentTime = connection.arrivalTime,
                    segments = state.segments + connection,
                    visitedStopIds = state.visitedStopIds + connection.toLocation.stopId,
                    totalDuration = state.totalDuration.plus(segmentDuration),
                    totalPrice = state.totalPrice + connection.price,
                )
                queue.add(nextState)
            }
        }

        return found.sortedWith(routeComparator(optimization))
    }

    override fun toRouteResponses(paths: List<RoutingPath>): List<RouteResponse> {
        return paths.map { path ->
            RouteResponse(
                totalDuration = path.totalDuration,
                totalPrice = path.totalPrice,
                segments = path.segments.map { segment ->
                    RouteSegment(
                        fromStopId = segment.fromLocation.stopId,
                        toStopId = segment.toLocation.stopId,
                        fromName = segment.fromLocation.name,
                        toName = segment.toLocation.name,
                        fromLat = segment.fromLocation.lat,
                        fromLon = segment.fromLocation.lon,
                        toLat = segment.toLocation.lat,
                        toLon = segment.toLocation.lon,
                        departureTime = segment.departureTime,
                        arrivalTime = segment.arrivalTime,
                        carrier = segment.carrier,
                        type = normalizeTransportType(segment.type) ?: TransportType.BUS,
                        geometry = resolveGeometry(segment),
                    )
                }
            )
        }
    }

    private fun resolveGeometry(segment: Connection): List<RouteGeometryPoint> {
        val shapeId = segment.shapeId ?: return fallbackGeometry(segment)
        val fromDistance = segment.fromShapeDistTraveled ?: return fallbackGeometry(segment)
        val toDistance = segment.toShapeDistTraveled ?: return fallbackGeometry(segment)

        val start = minOf(fromDistance, toDistance)
        val end = maxOf(fromDistance, toDistance)
        val shapePoints = shapePointRepo
            .findByShapeIdAndShapeDistTraveledBetweenOrderByShapeDistTraveledAscShapePtSequenceAsc(
                shapeId = shapeId,
                fromDistance = start,
                toDistance = end,
            )
            .map { point -> RouteGeometryPoint(lat = point.lat, lon = point.lon) }
            .let { points -> if (fromDistance <= toDistance) points else points.asReversed() }

        return shapePoints.ifEmpty { fallbackGeometry(segment) }
    }

    private fun fallbackGeometry(segment: Connection): List<RouteGeometryPoint> {
        return listOf(
            RouteGeometryPoint(segment.fromLocation.lat, segment.fromLocation.lon),
            RouteGeometryPoint(segment.toLocation.lat, segment.toLocation.lon),
        )
    }

    private fun routeComparator(optimization: RouteOptimization): Comparator<RoutingPath> {
        return when (optimization) {
            RouteOptimization.FASTEST -> compareBy<RoutingPath>(
                { it.totalDuration },
                { it.totalPrice }
            )
            RouteOptimization.CHEAPEST -> compareBy<RoutingPath>(
                { it.totalPrice },
                { it.totalDuration }
            )
        }
    }

    private fun stateComparator(optimization: RouteOptimization): Comparator<SearchState> {
        return when (optimization) {
            RouteOptimization.FASTEST -> compareBy<SearchState>(
                { it.totalDuration },
                { it.totalPrice }
            )
            RouteOptimization.CHEAPEST -> compareBy<SearchState>(
                { it.totalPrice },
                { it.totalDuration }
            )
        }
    }

    private fun costDominates(
        bestKnown: SearchCost,
        candidate: SearchCost,
        optimization: RouteOptimization,
    ): Boolean {
        return when (optimization) {
            RouteOptimization.FASTEST ->
                bestKnown.totalDuration <= candidate.totalDuration && bestKnown.totalPrice <= candidate.totalPrice
            RouteOptimization.CHEAPEST ->
                bestKnown.totalPrice <= candidate.totalPrice && bestKnown.totalDuration <= candidate.totalDuration
        }
    }

    private fun normalizeTransportType(rawType: String): TransportType? {
        return when (rawType.trim().uppercase()) {
            "BUS", "PUBLIC_TRANSPORT" -> TransportType.BUS
            "TRAIN" -> TransportType.TRAIN
            "METRO", "SUBWAY" -> TransportType.METRO
            else -> null
        }
    }

    private fun normalizeRequestTypes(types: Set<TransportType>): List<String> {
        val normalized = normalizeRequestTypeSet(types)
        val rawTypes = normalized
            .flatMap { type ->
                when (type) {
                    TransportType.BUS -> listOf("BUS", "PUBLIC_TRANSPORT")
                    TransportType.TRAIN -> listOf("TRAIN")
                    TransportType.METRO -> listOf("METRO", "SUBWAY")
                }
            }
            .toSet()
        return rawTypes.sorted()
    }

    private fun normalizeRequestTypeSet(types: Set<TransportType>): Set<TransportType> {
        return types.ifEmpty { setOf(TransportType.BUS) }
    }

    companion object {
        // Max 3 transfers means max 4 segments in the path.
        private const val MAX_SEGMENTS = 4
        private const val STEP_CANDIDATES_LIMIT = 300
        private const val MAX_RESULTS = 5
    }

    private data class SearchState(
        val currentStopId: String,
        val currentLocationId: java.util.UUID,
        val currentTime: OffsetDateTime,
        val segments: List<Connection>,
        val visitedStopIds: Set<String>,
        val totalDuration: Duration,
        val totalPrice: BigDecimal,
    )

    private data class SearchCost(
        val totalDuration: Duration,
        val totalPrice: BigDecimal,
    )
}
