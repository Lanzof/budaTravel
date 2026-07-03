package io.lanzof.core.service

import io.lanzof.dto.RouteOptimization
import io.lanzof.dto.RouteResponse
import io.lanzof.dto.TransportType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.time.OffsetDateTime

class RouteServiceImplTest {
    private val routingService = mock(RoutingService::class.java)
    private val routeService = RouteServiceImpl(routingService)

    @Test
    fun `findRoutes delegates to routing service and maps responses`() {
        val departure = OffsetDateTime.parse("2026-01-26T08:03:00+01:00")
        val paths = emptyList<RoutingPath>()
        val responses = emptyList<RouteResponse>()
        `when`(
            routingService.findRoutes(
                "F04181",
                "F04526",
                departure,
                RouteOptimization.FASTEST,
                setOf(TransportType.BUS),
            )
        ).thenReturn(paths)
        `when`(routingService.toRouteResponses(paths)).thenReturn(responses)

        val actual = routeService.findRoutes(
            from = "F04181",
            to = "F04526",
            departureDateTime = departure,
            optimization = RouteOptimization.FASTEST,
            transportTypes = listOf(TransportType.BUS),
        )

        assertEquals(responses, actual)
        verify(routingService).toRouteResponses(paths)
    }
}

