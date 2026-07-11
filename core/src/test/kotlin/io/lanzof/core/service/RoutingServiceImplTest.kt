package io.lanzof.core.service

import io.lanzof.core.entity.Connection
import io.lanzof.core.entity.GtfsShapePoint
import io.lanzof.core.entity.Location
import io.lanzof.core.repo.ConnectionRepo
import io.lanzof.core.repo.GtfsShapePointRepo
import io.lanzof.core.repo.LocationRepo
import io.lanzof.dto.RouteOptimization
import io.lanzof.dto.TransportType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class RoutingServiceImplTest {
    private val locationRepo = mock(LocationRepo::class.java)
    private val connectionRepo = mock(ConnectionRepo::class.java)
    private val shapePointRepo = mock(GtfsShapePointRepo::class.java)
    private val routingService = RoutingServiceImpl(locationRepo, connectionRepo, shapePointRepo)

    @Test
    fun `fastest best-first picks fastest valid branch and ignores over-transfer branch`() {
        val a = stop("A")
        val b = stop("B")
        val c1 = stop("C1")
        val d1 = stop("D1")
        val c2 = stop("C2")
        val d2 = stop("D2")
        val e2 = stop("E2")
        val f2 = stop("F2")
        val g2 = stop("G2")
        val start = t("2026-01-26T08:00:00+01:00")

        // Branch 1 (valid, fastest): A -> C1 -> D1 -> B (2 transfers)
        val aC1 = connection(a, c1, "2026-01-26T08:00:00+01:00", "2026-01-26T08:10:00+01:00", "2.0", "BUS")
        val c1D1 = connection(c1, d1, "2026-01-26T08:10:00+01:00", "2026-01-26T08:20:00+01:00", "2.0", "BUS")
        val d1B = connection(d1, b, "2026-01-26T08:20:00+01:00", "2026-01-26T08:30:00+01:00", "2.0", "BUS")

        // Branch 2 (valid, slower): A -> C2 -> B (1 transfer)
        val aC2 = connection(a, c2, "2026-01-26T08:01:00+01:00", "2026-01-26T08:20:00+01:00", "1.0", "BUS")
        val c2B = connection(c2, b, "2026-01-26T08:21:00+01:00", "2026-01-26T08:50:00+01:00", "1.0", "BUS")

        // Branch 3 (invalid, 4 transfers): A -> D2 -> E2 -> F2 -> G2 -> B
        val aD2 = connection(a, d2, "2026-01-26T08:00:00+01:00", "2026-01-26T08:05:00+01:00", "0.0", "BUS")
        val d2E2 = connection(d2, e2, "2026-01-26T08:05:00+01:00", "2026-01-26T08:10:00+01:00", "0.0", "BUS")
        val e2F2 = connection(e2, f2, "2026-01-26T08:10:00+01:00", "2026-01-26T08:15:00+01:00", "0.0", "BUS")
        val f2G2 = connection(f2, g2, "2026-01-26T08:15:00+01:00", "2026-01-26T08:20:00+01:00", "0.0", "BUS")
        val g2B = connection(g2, b, "2026-01-26T08:20:00+01:00", "2026-01-26T08:25:00+01:00", "0.0", "BUS")

        stubStops(a, b)
        stubNext(a, start, listOf(aC1, aC2, aD2))
        stubNext(c1, t("2026-01-26T08:10:00+01:00"), listOf(c1D1))
        stubNext(d1, t("2026-01-26T08:20:00+01:00"), listOf(d1B))
        stubNext(c2, t("2026-01-26T08:20:00+01:00"), listOf(c2B))
        stubNext(d2, t("2026-01-26T08:05:00+01:00"), listOf(d2E2))
        stubNext(e2, t("2026-01-26T08:10:00+01:00"), listOf(e2F2))
        stubNext(f2, t("2026-01-26T08:15:00+01:00"), listOf(f2G2))
        stubNext(g2, t("2026-01-26T08:20:00+01:00"), listOf(g2B))

        val paths = routingService.findRoutes(
            originStopId = "A",
            destinationStopId = "B",
            departureDateTime = start,
            optimization = RouteOptimization.FASTEST,
            transportTypes = setOf(TransportType.BUS),
        )

        assertTrue(paths.isNotEmpty())
        // Fastest valid branch must be selected first.
        assertEquals(listOf("A", "C1", "D1"), paths.first().segments.map { it.fromLocation.stopId })
        assertEquals("B", paths.first().segments.last().toLocation.stopId)
        // Ensure over-transfer branch is ignored.
        assertTrue(paths.none { it.segments.size > 4 })
    }

    @Test
    fun `zero seconds segment is normalized to one minute`() {
        val a = stop("A")
        val b = stop("B")
        val start = t("2026-01-26T08:00:00+01:00")
        val zeroDuration = connection(a, b, "2026-01-26T08:03:00+01:00", "2026-01-26T08:03:00+01:00", "0.0", "BUS")

        stubStops(a, b)
        stubNext(a, start, listOf(zeroDuration))

        val paths = routingService.findRoutes("A", "B", start)
        val responses = routingService.toRouteResponses(paths)

        assertEquals("PT1M", paths.first().totalDuration.toString())
        assertEquals("PT1M", responses.first().totalDuration.toString())
    }

    @Test
    fun `route response uses shape geometry when connection has GTFS shape metadata`() {
        val a = stop("A", lat = 47.0, lon = 19.0)
        val b = stop("B", lat = 48.0, lon = 20.0)
        val start = t("2026-01-26T08:00:00+01:00")
        val shapedConnection = Connection(
            id = UUID.randomUUID(),
            fromLocation = a,
            toLocation = b,
            departureTime = t("2026-01-26T08:00:00+01:00"),
            arrivalTime = t("2026-01-26T08:10:00+01:00"),
            price = BigDecimal.ZERO,
            carrier = "BKK",
            type = "BUS",
            tripId = "trip-1",
            routeId = "route-1",
            shapeId = "shape-1",
            fromStopSequence = 1,
            toStopSequence = 2,
            fromShapeDistTraveled = 0.0,
            toShapeDistTraveled = 20.0,
        )

        stubStops(a, b)
        stubNext(a, start, listOf(shapedConnection))
        `when`(
            shapePointRepo.findByShapeIdAndShapeDistTraveledBetweenOrderByShapeDistTraveledAscShapePtSequenceAsc(
                "shape-1",
                0.0,
                20.0,
            )
        ).thenReturn(
            listOf(
                GtfsShapePoint(shapeId = "shape-1", shapePtSequence = 1, lat = 47.1, lon = 19.1, shapeDistTraveled = 1.0),
                GtfsShapePoint(shapeId = "shape-1", shapePtSequence = 2, lat = 47.2, lon = 19.2, shapeDistTraveled = 10.0),
            )
        )

        val responses = routingService.toRouteResponses(routingService.findRoutes("A", "B", start))

        assertEquals(2, responses.first().segments.first().geometry.size)
        assertEquals(47.1, responses.first().segments.first().geometry.first().lat)
        assertEquals(19.2, responses.first().segments.first().geometry.last().lon)
    }

    @Test
    fun `routing uses repository next-hop query and not full dump`() {
        val a = stop("A")
        val b = stop("B")
        val start = t("2026-01-26T08:00:00+01:00")

        stubStops(a, b)
        stubNext(a, start, emptyList())

        val result = routingService.findRoutes("A", "B", start)

        assertTrue(result.isEmpty())
        verify(connectionRepo, never()).findAll()
    }

    private fun stubStops(origin: Location, destination: Location) {
        `when`(locationRepo.findByStopId(origin.stopId)).thenReturn(listOf(origin))
        `when`(locationRepo.findByStopId(destination.stopId)).thenReturn(listOf(destination))
    }

    private fun stubNext(from: Location, fromTime: OffsetDateTime, connections: List<Connection>) {
        `when`(
            connectionRepo.findNextConnections(
                from.id!!,
                fromTime,
                listOf("BUS", "PUBLIC_TRANSPORT"),
                PageRequest.of(0, 300),
            )
        ).thenReturn(connections)
    }

    private fun stop(id: String, lat: Double = 47.0, lon: Double = 19.0): Location {
        return Location(
            id = UUID.randomUUID(),
            stopId = id,
            name = "Stop $id",
            lat = lat,
            lon = lon,
        )
    }

    private fun connection(
        from: Location,
        to: Location,
        departure: String,
        arrival: String,
        price: String,
        type: String,
    ): Connection {
        return Connection(
            id = UUID.randomUUID(),
            fromLocation = from,
            toLocation = to,
            departureTime = t(departure),
            arrivalTime = t(arrival),
            price = BigDecimal(price),
            carrier = "BKK",
            type = type,
        )
    }

    private fun t(value: String): OffsetDateTime = OffsetDateTime.parse(value)
}
