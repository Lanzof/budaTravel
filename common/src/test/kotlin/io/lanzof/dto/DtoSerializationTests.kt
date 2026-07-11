package io.lanzof.dto

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime

class DtoSerializationTests {
    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(
            KotlinModule.Builder().build()
        )
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)

    @Test
    fun `location dto serializes with expected fields`() {
        val dto = LocationDto(
            stopId = "ABC123",
            name = "Budapest, Example Stop",
            lat = 47.4979,
            lon = 19.0402,
        )

        val json = objectMapper.writeValueAsString(dto)

        assertTrue(json.contains("\"stopId\":\"ABC123\""))
        assertTrue(json.contains("\"name\":\"Budapest, Example Stop\""))
        assertTrue(json.contains("\"lat\":47.4979"))
        assertTrue(json.contains("\"lon\":19.0402"))
    }

    @Test
    fun `location suggestion dto serializes with expected fields`() {
        val dto = LocationSuggestionDto(
            stopId = "ABC123",
            name = "Budapest, Example Stop",
            lat = 47.4979,
            lon = 19.0402,
        )

        val json = objectMapper.writeValueAsString(dto)

        assertTrue(json.contains("\"stopId\":\"ABC123\""))
        assertTrue(json.contains("\"name\":\"Budapest, Example Stop\""))
        assertTrue(json.contains("\"lat\":47.4979"))
        assertTrue(json.contains("\"lon\":19.0402"))
    }

    @Test
    fun `route search request serializes with ISO datetime and enum values`() {
        val request = RouteSearchRequest(
            originStopId = "ORIGIN_STOP",
            destinationStopId = "DEST_STOP",
            departureDateTime = OffsetDateTime.parse("2025-01-10T14:30:00+01:00"),
            optimization = RouteOptimization.CHEAPEST,
            transportTypes = listOf(TransportType.BUS, TransportType.METRO),
        )

        val json = objectMapper.writeValueAsString(request)

        assertTrue(json.contains("\"originStopId\":\"ORIGIN_STOP\""))
        assertTrue(json.contains("\"destinationStopId\":\"DEST_STOP\""))
        assertTrue(json.contains("\"departureDateTime\":\"2025-01-10T14:30:00+01:00\""))
        assertTrue(json.contains("\"optimization\":\"CHEAPEST\""))
        assertTrue(json.contains("\"transportTypes\":[\"BUS\",\"METRO\"]"))
    }

    @Test
    fun `route response deserializes nested segment structure`() {
        val json = """
            {
              "totalDuration": "PT1H30M",
              "totalPrice": 12.50,
              "segments": [
                {
                  "from": {
                    "stopId": "STOP_A",
                    "name": "Budapest, Stop A",
                    "lat": 47.4979,
                    "lon": 19.0402
                  },
                  "to": {
                    "stopId": "STOP_B",
                    "name": "Budapest, Stop B",
                    "lat": 47.4985,
                    "lon": 19.0450
                  },
                  "timing": {
                    "departureTime": "2025-01-10T14:30:00+01:00",
                    "arrivalTime": "2025-01-10T14:50:00+01:00"
                  },
                  "transport": {
                    "carrier": "BKK",
                    "type": "BUS",
                    "routeId": "0160"
                  },
                  "gtfs": {
                    "tripId": "D075211",
                    "shapeId": "CB58",
                    "fromStopSequence": 1,
                    "toStopSequence": 2
                  },
                  "geometry": [
                    { "lat": 47.4979, "lon": 19.0402 },
                    { "lat": 47.4985, "lon": 19.0450 }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed: RouteResponse = objectMapper.readValue(json)

        assertEquals(Duration.ofMinutes(90), parsed.totalDuration)
        assertEquals(BigDecimal("12.50"), parsed.totalPrice)
        assertEquals(1, parsed.segments.size)

        val segment = parsed.segments.single()
        assertEquals(RouteStop("STOP_A", "Budapest, Stop A", 47.4979, 19.0402), segment.from)
        assertEquals(RouteStop("STOP_B", "Budapest, Stop B", 47.4985, 19.0450), segment.to)
        assertEquals(
            RouteSegmentTiming(
                departureTime = OffsetDateTime.parse("2025-01-10T14:30:00+01:00"),
                arrivalTime = OffsetDateTime.parse("2025-01-10T14:50:00+01:00"),
            ),
            segment.timing,
        )
        assertEquals(RouteTransport("BKK", TransportType.BUS, "0160"), segment.transport)
        assertEquals(
            GtfsSegmentMetadata(
                tripId = "D075211",
                shapeId = "CB58",
                fromStopSequence = 1,
                toStopSequence = 2,
            ),
            segment.gtfs,
        )
        assertEquals(
            listOf(
                RouteGeometryPoint(47.4979, 19.0402),
                RouteGeometryPoint(47.4985, 19.0450),
            ),
            segment.geometry,
        )
    }

    @Test
    fun `route search request defaults align with spec`() {
        val json = """
            {
              "originStopId": "A",
              "destinationStopId": "B",
              "departureDateTime": "2025-01-10T14:30:00+01:00"
            }
        """.trimIndent()

        val parsed: RouteSearchRequest = objectMapper.readValue(json)

        assertEquals(RouteOptimization.FASTEST, parsed.optimization)
        assertEquals(listOf(TransportType.BUS), parsed.transportTypes)
    }
}

