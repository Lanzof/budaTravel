package io.lanzof.api

import io.lanzof.api.dto.ApiErrorResponse
import io.lanzof.api.dto.ReadinessResponse
import io.lanzof.core.entity.Connection
import io.lanzof.core.entity.Location
import io.lanzof.core.repo.ConnectionRepo
import io.lanzof.core.repo.LocationRepo
import io.lanzof.core.repo.ImportStatusRepo
import io.lanzof.core.service.ImportStatusService
import io.lanzof.dto.LocationDto
import io.lanzof.dto.LocationSuggestionDto
import io.lanzof.dto.RouteOptimization
import io.lanzof.dto.RouteResponse
import io.lanzof.dto.RouteSearchRequest
import io.lanzof.dto.TransportType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.OffsetDateTime

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class ApiApplicationTests {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var locationRepo: LocationRepo

    @Autowired
    private lateinit var connectionRepo: ConnectionRepo

    @Autowired
    private lateinit var importStatusRepo: ImportStatusRepo

    @Autowired
    private lateinit var importStatusService: ImportStatusService

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        importStatusRepo.deleteAll()
        connectionRepo.deleteAll()
        locationRepo.deleteAll()
    }

    @Test
    fun `health endpoint should return OK`() {
        val url = "http://localhost:$port/api/v1/routes/health"

        val response = restTemplate.getForEntity(url, String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("API is running", response.body)
    }

    @Test
    fun `readiness endpoint should return 503 before demo data import is complete`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/readiness",
            ReadinessResponse::class.java,
        )

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(false, response.body!!.ready)
        assertEquals(ImportStatusService.DEMO_DATASET_NAME, response.body!!.datasetName)
        assertEquals(0, response.body!!.locationsCount)
        assertEquals(0, response.body!!.connectionsCount)
    }

    @Test
    fun `readiness endpoint should return 200 after demo data import is complete`() {
        val a = locationRepo.save(Location(stopId = "A", name = "Stop A", lat = 47.0, lon = 19.0))
        val b = locationRepo.save(Location(stopId = "B", name = "Stop B", lat = 47.1, lon = 19.1))
        connectionRepo.save(conn(a, b, "2026-01-26T08:00:00+01:00", "2026-01-26T08:15:00+01:00", "0.0"))
        importStatusService.markCompleted()

        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/readiness",
            ReadinessResponse::class.java,
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(true, response.body!!.ready)
        assertEquals("COMPLETED", response.body!!.status!!.name)
        assertEquals(2, response.body!!.locationsCount)
        assertEquals(1, response.body!!.connectionsCount)
    }

    @Test
    fun `CORS preflight should allow local web dev origin`() {
        val headers = HttpHeaders().apply {
            origin = "http://localhost:5173"
            accessControlRequestMethod = HttpMethod.GET
        }

        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/locations",
            HttpMethod.OPTIONS,
            HttpEntity(null, headers),
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("http://localhost:5173", response.headers.accessControlAllowOrigin)
        assertTrue(response.headers.accessControlAllowMethods.contains(HttpMethod.GET))
    }

    @Test
    fun `GET locations without params should return data`() {
        locationRepo.saveAll(sampleStops())

        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/locations",
            HttpMethod.GET,
            null,
            LOCATION_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(3, response.body!!.size)
    }

    @Test
    fun `GET locations with q should filter by name`() {
        locationRepo.saveAll(sampleStops())

        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/locations?q=puskas",
            HttpMethod.GET,
            null,
            LOCATION_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body!!.size)
        assertEquals("003002", response.body!![0].stopId)
    }

    @Test
    fun `GET locations with stopId should filter by stopId`() {
        locationRepo.saveAll(sampleStops())

        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/locations?stopId=002138",
            HttpMethod.GET,
            null,
            LOCATION_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body!!.size)
        assertEquals("002138", response.body!![0].stopId)
    }

    @Test
    fun `GET locations with bbox should return stops inside bounds`() {
        locationRepo.saveAll(sampleStops())

        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/locations?minLat=47.49&maxLat=47.51&minLon=19.10&maxLon=19.14",
            HttpMethod.GET,
            null,
            LOCATION_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(listOf("002133", "003002"), response.body!!.map { it.stopId })
    }

    @Test
    fun `GET locations with partial bbox should return 400`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations?minLat=47.49&maxLat=47.51",
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `GET locations with invalid bbox range should return 400`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations?minLat=48&maxLat=47&minLon=19&maxLon=20",
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `GET locations with both q and stopId should return 400`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations?q=puskas&stopId=003002",
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `GET locations with invalid limit should return 400`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations?limit=0",
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `GET locations with invalid offset should return 400`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations?offset=-1",
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `GET locations with non numeric limit should return 400`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations?limit=abc",
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `GET location by stopId should return 200 with payload`() {
        locationRepo.save(
            Location(
                stopId = "003002",
                name = "Puskas Ferenc Stadion M",
                lat = 47.500368,
                lon = 19.103406
            )
        )

        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations/003002",
            LocationDto::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("003002", response.body!!.stopId)
        assertEquals("Puskas Ferenc Stadion M", response.body!!.name)
    }

    @Test
    fun `GET location by stopId should return 404 when missing`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations/UNKNOWN",
            String::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `GET locations autocomplete should return default limited results`() {
        val locations = (1..30).map {
            Location(
                stopId = "F${it.toString().padStart(5, '0')}",
                name = "Budapest, Example Stop $it",
                lat = 47.0 + (it / 1000.0),
                lon = 19.0 + (it / 1000.0),
            )
        }
        locationRepo.saveAll(locations)

        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/locations/autocomplete?q=budapest",
            HttpMethod.GET,
            null,
            LOCATION_SUGGESTION_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(10, response.body!!.size)
    }

    @Test
    fun `GET locations autocomplete should ignore accents`() {
        locationRepo.save(
            Location(
                stopId = "F00985",
                name = "Deák Ferenc tér M",
                lat = 47.497701,
                lon = 19.053353,
            )
        )

        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/locations/autocomplete?q=Deak&limit=10",
            HttpMethod.GET,
            null,
            LOCATION_SUGGESTION_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body!!.size)
        assertEquals("F00985", response.body!!.first().stopId)
    }

    @Test
    fun `GET locations autocomplete should cap limit to 50`() {
        val locations = (1..60).map {
            Location(
                stopId = "F${it.toString().padStart(5, '0')}",
                name = "Budapest, Example Stop $it",
                lat = 47.0 + (it / 1000.0),
                lon = 19.0 + (it / 1000.0),
            )
        }
        locationRepo.saveAll(locations)

        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/locations/autocomplete?q=budapest&limit=999",
            HttpMethod.GET,
            null,
            LOCATION_SUGGESTION_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(50, response.body!!.size)
    }

    @Test
    fun `GET locations autocomplete should return 400 when q is missing`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations/autocomplete",
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `GET locations autocomplete should return 400 when q is blank`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/locations/autocomplete?q=   ",
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `POST route search should use fastest branch and ignore branch with 4 transfers`() {
        val a = locationRepo.save(Location(stopId = "A", name = "Stop A", lat = 47.0, lon = 19.0))
        val b = locationRepo.save(Location(stopId = "B", name = "Stop B", lat = 47.1, lon = 19.1))
        val c1 = locationRepo.save(Location(stopId = "C1", name = "Stop C1", lat = 47.2, lon = 19.2))
        val d1 = locationRepo.save(Location(stopId = "D1", name = "Stop D1", lat = 47.3, lon = 19.3))
        val c2 = locationRepo.save(Location(stopId = "C2", name = "Stop C2", lat = 47.4, lon = 19.4))
        val d2 = locationRepo.save(Location(stopId = "D2", name = "Stop D2", lat = 47.5, lon = 19.5))
        val e2 = locationRepo.save(Location(stopId = "E2", name = "Stop E2", lat = 47.6, lon = 19.6))
        val f2 = locationRepo.save(Location(stopId = "F2", name = "Stop F2", lat = 47.7, lon = 19.7))
        val g2 = locationRepo.save(Location(stopId = "G2", name = "Stop G2", lat = 47.8, lon = 19.8))

        connectionRepo.saveAll(
            listOf(
                // Branch 1 (valid, fastest): A -> C1 -> D1 -> B
                conn(a, c1, "2026-01-26T08:00:00+01:00", "2026-01-26T08:10:00+01:00", "2.0"),
                conn(c1, d1, "2026-01-26T08:10:00+01:00", "2026-01-26T08:20:00+01:00", "2.0"),
                conn(d1, b, "2026-01-26T08:20:00+01:00", "2026-01-26T08:30:00+01:00", "2.0"),
                // Branch 2 (valid, slower): A -> C2 -> B
                conn(a, c2, "2026-01-26T08:01:00+01:00", "2026-01-26T08:20:00+01:00", "1.0"),
                conn(c2, b, "2026-01-26T08:21:00+01:00", "2026-01-26T08:50:00+01:00", "1.0"),
                // Branch 3 (invalid, 4 transfers): A -> D2 -> E2 -> F2 -> G2 -> B
                conn(a, d2, "2026-01-26T08:00:00+01:00", "2026-01-26T08:05:00+01:00", "0.0"),
                conn(d2, e2, "2026-01-26T08:05:00+01:00", "2026-01-26T08:10:00+01:00", "0.0"),
                conn(e2, f2, "2026-01-26T08:10:00+01:00", "2026-01-26T08:15:00+01:00", "0.0"),
                conn(f2, g2, "2026-01-26T08:15:00+01:00", "2026-01-26T08:20:00+01:00", "0.0"),
                conn(g2, b, "2026-01-26T08:20:00+01:00", "2026-01-26T08:25:00+01:00", "0.0"),
            )
        )

        val request = RouteSearchRequest(
            originStopId = "A",
            destinationStopId = "B",
            departureDateTime = OffsetDateTime.parse("2026-01-26T08:00:00+01:00"),
            optimization = RouteOptimization.FASTEST,
            transportTypes = listOf(TransportType.BUS),
        )
        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/routes/search",
            HttpMethod.POST,
            HttpEntity(request),
            ROUTE_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val routes = response.body!!
        assertEquals(2, routes.size)
        assertEquals(3, routes.first().segments.size)
        assertEquals(listOf("A", "C1", "D1"), routes.first().segments.map { it.from.stopId })
        assertEquals("B", routes.first().segments.last().to.stopId)
        assertTrue(routes.none { it.segments.size > 4 })
    }

    @Test
    fun `POST route search should return direct route when available`() {
        val a = locationRepo.save(Location(stopId = "A", name = "Stop A", lat = 47.0, lon = 19.0))
        val b = locationRepo.save(Location(stopId = "B", name = "Stop B", lat = 47.1, lon = 19.1))

        connectionRepo.save(
            conn(
                a,
                b,
                "2026-01-26T08:00:00+01:00",
                "2026-01-26T08:15:00+01:00",
                "3.5",
            )
        )

        val request = RouteSearchRequest(
            originStopId = "A",
            destinationStopId = "B",
            departureDateTime = OffsetDateTime.parse("2026-01-26T08:00:00+01:00"),
            optimization = RouteOptimization.FASTEST,
            transportTypes = listOf(TransportType.BUS),
        )
        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/routes/search",
            HttpMethod.POST,
            HttpEntity(request),
            ROUTE_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body!!.size)
        assertEquals(1, response.body!![0].segments.size)
        assertEquals("A", response.body!![0].segments[0].from.stopId)
        assertEquals("B", response.body!![0].segments[0].to.stopId)
        assertEquals("Stop A", response.body!![0].segments[0].from.name)
        assertEquals("Stop B", response.body!![0].segments[0].to.name)
    }

    @Test
    fun `POST route search should return 422 when no route found`() {
        locationRepo.save(Location(stopId = "A", name = "Stop A", lat = 47.0, lon = 19.0))
        locationRepo.save(Location(stopId = "B", name = "Stop B", lat = 47.1, lon = 19.1))

        val request = RouteSearchRequest(
            originStopId = "A",
            destinationStopId = "B",
            departureDateTime = OffsetDateTime.parse("2026-01-26T08:00:00+01:00"),
            optimization = RouteOptimization.FASTEST,
            transportTypes = listOf(TransportType.BUS),
        )
        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/routes/search",
            HttpMethod.POST,
            HttpEntity(request),
            ApiErrorResponse::class.java
        )

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        assertEquals("NO_ROUTE_FOUND", response.body!!.code)
        assertEquals("No route found for the given parameters.", response.body!!.message)
    }

    @Test
    fun `POST route search should order routes by optimization preference`() {
        val a = locationRepo.save(Location(stopId = "A", name = "Stop A", lat = 47.0, lon = 19.0))
        val b = locationRepo.save(Location(stopId = "B", name = "Stop B", lat = 47.1, lon = 19.1))
        val c = locationRepo.save(Location(stopId = "C", name = "Stop C", lat = 47.2, lon = 19.2))
        val d = locationRepo.save(Location(stopId = "D", name = "Stop D", lat = 47.3, lon = 19.3))

        connectionRepo.saveAll(
            listOf(
                // Fast but expensive
                conn(a, c, "2026-01-26T08:00:00+01:00", "2026-01-26T08:10:00+01:00", "10.0"),
                conn(c, b, "2026-01-26T08:10:00+01:00", "2026-01-26T08:20:00+01:00", "10.0"),
                // Slower but cheap
                conn(a, d, "2026-01-26T08:00:00+01:00", "2026-01-26T08:15:00+01:00", "1.0"),
                conn(d, b, "2026-01-26T08:16:00+01:00", "2026-01-26T08:35:00+01:00", "1.0"),
            )
        )

        val fastestRequest = RouteSearchRequest(
            originStopId = "A",
            destinationStopId = "B",
            departureDateTime = OffsetDateTime.parse("2026-01-26T08:00:00+01:00"),
            optimization = RouteOptimization.FASTEST,
            transportTypes = listOf(TransportType.BUS),
        )
        val fastestResponse = restTemplate.exchange(
            "http://localhost:$port/api/v1/routes/search",
            HttpMethod.POST,
            HttpEntity(fastestRequest),
            ROUTE_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, fastestResponse.statusCode)
        assertEquals(listOf("A", "C"), fastestResponse.body!!.first().segments.map { it.from.stopId })

        val cheapestRequest = fastestRequest.copy(optimization = RouteOptimization.CHEAPEST)
        val cheapestResponse = restTemplate.exchange(
            "http://localhost:$port/api/v1/routes/search",
            HttpMethod.POST,
            HttpEntity(cheapestRequest),
            ROUTE_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, cheapestResponse.statusCode)
        assertEquals(listOf("A", "D"), cheapestResponse.body!!.first().segments.map { it.from.stopId })
    }

    @Test
    fun `POST route search should handle departureDateTime near end of day`() {
        val a = locationRepo.save(Location(stopId = "A", name = "Stop A", lat = 47.0, lon = 19.0))
        val b = locationRepo.save(Location(stopId = "B", name = "Stop B", lat = 47.1, lon = 19.1))

        connectionRepo.save(
            conn(
                a,
                b,
                "2026-01-26T23:59:00+01:00",
                "2026-01-27T00:05:00+01:00",
                "2.0",
            )
        )

        val request = RouteSearchRequest(
            originStopId = "A",
            destinationStopId = "B",
            departureDateTime = OffsetDateTime.parse("2026-01-26T23:59:00+01:00"),
            optimization = RouteOptimization.FASTEST,
            transportTypes = listOf(TransportType.BUS),
        )
        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/routes/search",
            HttpMethod.POST,
            HttpEntity(request),
            ROUTE_LIST_TYPE
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body!!.size)
        assertEquals(1, response.body!![0].segments.size)
        assertEquals(
            OffsetDateTime.parse("2026-01-27T00:05:00+01:00").toInstant(),
            response.body!![0].segments[0].timing.arrivalTime.toInstant()
        )
    }

    @Test
    fun `POST route search should return 400 when origin is missing`() {
        val payload = """
            {
              "destinationStopId": "B",
              "departureDateTime": "2026-01-26T08:00:00+01:00",
              "optimization": "FASTEST",
              "transportTypes": ["BUS"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `POST route search should return 400 when destination is missing`() {
        val payload = """
            {
              "originStopId": "A",
              "departureDateTime": "2026-01-26T08:00:00+01:00",
              "optimization": "FASTEST",
              "transportTypes": ["BUS"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `POST route search should return 400 when origin equals destination`() {
        val payload = """
            {
              "originStopId": "A",
              "destinationStopId": "A",
              "departureDateTime": "2026-01-26T08:00:00+01:00",
              "optimization": "FASTEST",
              "transportTypes": ["BUS"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `POST route search should return 400 when departureDateTime is missing`() {
        val payload = """
            {
              "originStopId": "A",
              "destinationStopId": "B",
              "optimization": "FASTEST",
              "transportTypes": ["BUS"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `POST route search should return 400 for invalid departureDateTime`() {
        val payload = """
            {
              "originStopId": "A",
              "destinationStopId": "B",
              "departureDateTime": "2026/01/26 08:00:00",
              "optimization": "FASTEST",
              "transportTypes": ["BUS"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `POST route search should return 400 for unsupported optimization`() {
        val payload = """
            {
              "originStopId": "A",
              "destinationStopId": "B",
              "departureDateTime": "2026-01-26T08:00:00+01:00",
              "optimization": "SHORTEST",
              "transportTypes": ["BUS"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `POST route search should return 400 for unsupported transport type`() {
        val payload = """
            {
              "originStopId": "A",
              "destinationStopId": "B",
              "departureDateTime": "2026-01-26T08:00:00+01:00",
              "optimization": "FASTEST",
              "transportTypes": ["PLANE"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `POST route search should return 404 when origin stop does not exist`() {
        locationRepo.save(Location(stopId = "B", name = "Stop B", lat = 47.1, lon = 19.1))
        val payload = """
            {
              "originStopId": "UNKNOWN",
              "destinationStopId": "B",
              "departureDateTime": "2026-01-26T08:00:00+01:00",
              "optimization": "FASTEST",
              "transportTypes": ["BUS"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `POST route search should return 404 when destination stop does not exist`() {
        locationRepo.save(Location(stopId = "A", name = "Stop A", lat = 47.0, lon = 19.0))
        val payload = """
            {
              "originStopId": "A",
              "destinationStopId": "UNKNOWN",
              "departureDateTime": "2026-01-26T08:00:00+01:00",
              "optimization": "FASTEST",
              "transportTypes": ["BUS"]
            }
        """.trimIndent()
        val response = postRouteSearchJson(payload)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    companion object {
        private fun sampleStops(): List<Location> = listOf(
            Location(stopId = "002133", name = "Ors vezer tere M+H, deli taroloter", lat = 47.500366, lon = 19.135700),
            Location(stopId = "002138", name = "Kobanya also vasutallomas", lat = 47.483139, lon = 19.127891),
            Location(stopId = "003002", name = "Puskas Ferenc Stadion M", lat = 47.500368, lon = 19.103406),
        )

        private val LOCATION_LIST_TYPE = object : ParameterizedTypeReference<List<LocationDto>>() {}
        private val LOCATION_SUGGESTION_LIST_TYPE = object : ParameterizedTypeReference<List<LocationSuggestionDto>>() {}
        private val ROUTE_LIST_TYPE = object : ParameterizedTypeReference<List<RouteResponse>>() {}

        private fun conn(
            from: Location,
            to: Location,
            departure: String,
            arrival: String,
            price: String,
        ): Connection {
            return Connection(
                fromLocation = from,
                toLocation = to,
                departureTime = OffsetDateTime.parse(departure),
                arrivalTime = OffsetDateTime.parse(arrival),
                price = BigDecimal(price),
                carrier = "BKK",
                type = "BUS",
            )
        }
    }

    private fun postRouteSearchJson(payload: String) = restTemplate.exchange(
        "http://localhost:$port/api/v1/routes/search",
        HttpMethod.POST,
        HttpEntity(
            payload,
            HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        ),
        String::class.java
    )
}
