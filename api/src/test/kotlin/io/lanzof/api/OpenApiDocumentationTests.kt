package io.lanzof.api

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiDocumentationTests {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `openapi docs should contain key paths and schemas`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/v3/api-docs",
            String::class.java
        )

        val body = response.body ?: ""
        assertTrue(response.statusCode == HttpStatus.OK)
        assertTrue(body.contains("/api/v1/locations"))
        assertTrue(body.contains("/api/v1/locations/{stopId}"))
        assertTrue(body.contains("/api/v1/locations/autocomplete"))
        assertTrue(body.contains("/api/v1/routes/search"))

        assertTrue(body.contains("LocationDto"))
        assertTrue(body.contains("LocationSuggestionDto"))
        assertTrue(body.contains("RouteSearchRequest"))
        assertTrue(body.contains("RouteResponse"))
        assertTrue(body.contains("RouteSegment"))
        assertTrue(body.contains("ApiErrorResponse"))
    }
}
