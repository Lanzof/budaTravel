package io.lanzof.api.controller

import io.lanzof.api.exception.InvalidQueryParametersException
import io.lanzof.api.exception.LocationNotFoundException
import io.lanzof.api.dto.ApiErrorResponse
import io.lanzof.core.entity.Location
import io.lanzof.core.repo.LocationRepo
import io.lanzof.dto.LocationDto
import io.lanzof.dto.LocationSuggestionDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("/api/v1/locations")
@Tag(name = "Locations", description = "Locations and stop lookup endpoints")
class LocationController(
    private val locationRepo: LocationRepo,
) {
    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete locations by text")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Autocomplete results"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid query parameters",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
        ]
    )
    fun autocomplete(
        @RequestParam @NotBlank q: String,
        @RequestParam(defaultValue = "10") @Positive limit: Int,
    ): List<LocationSuggestionDto> {
        val effectiveLimit = minOf(limit, 50)
        return locationRepo.findByNameContainingIgnoreCase(q)
            .take(effectiveLimit)
            .map { it.toSuggestionDto() }
    }

    @GetMapping("/{stopId}")
    @Operation(summary = "Get a location by stopId")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Location found"),
            ApiResponse(
                responseCode = "404",
                description = "Location not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
        ]
    )
    fun getLocationByStopId(
        @PathVariable stopId: String,
    ): LocationDto {
        val location = locationRepo.findByStopId(stopId).firstOrNull()
            ?: throw LocationNotFoundException("Location with stopId '$stopId' not found.")
        return location.toDto()
    }

    @GetMapping
    @Operation(summary = "List locations with optional filters")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Locations list"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid query parameters",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]
            ),
        ]
    )
    fun getLocations(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) stopId: String?,
        @RequestParam(defaultValue = "50") @Positive @Max(200) limit: Int,
        @RequestParam(defaultValue = "0") @PositiveOrZero offset: Int,
        @RequestParam(required = false) minLat: Double?,
        @RequestParam(required = false) maxLat: Double?,
        @RequestParam(required = false) minLon: Double?,
        @RequestParam(required = false) maxLon: Double?,
    ): List<LocationDto> {
        if (q != null && stopId != null) {
            throw InvalidQueryParametersException("Parameters 'q' and 'stopId' cannot be used together.")
        }
        validateBoundingBox(minLat, maxLat, minLon, maxLon)

        val locations = when {
            q != null -> locationRepo.findByNameContainingIgnoreCase(q)
            stopId != null -> locationRepo.findByStopId(stopId)
            else -> locationRepo.findAll()
        }

        return locations
            .asSequence()
            .filter { it.isInsideBoundingBox(minLat, maxLat, minLon, maxLon) }
            .drop(offset)
            .take(limit)
            .map { it.toDto() }
            .toList()
    }

    private fun validateBoundingBox(minLat: Double?, maxLat: Double?, minLon: Double?, maxLon: Double?) {
        val values = listOf(minLat, maxLat, minLon, maxLon)
        if (values.any { it != null } && values.any { it == null }) {
            throw InvalidQueryParametersException(
                "Bounding box parameters minLat, maxLat, minLon and maxLon must be provided together."
            )
        }
        if (minLat != null && maxLat != null && minLat > maxLat) {
            throw InvalidQueryParametersException("Parameter 'minLat' cannot be greater than 'maxLat'.")
        }
        if (minLon != null && maxLon != null && minLon > maxLon) {
            throw InvalidQueryParametersException("Parameter 'minLon' cannot be greater than 'maxLon'.")
        }
    }

    private fun Location.isInsideBoundingBox(
        minLat: Double?,
        maxLat: Double?,
        minLon: Double?,
        maxLon: Double?,
    ): Boolean {
        if (minLat == null || maxLat == null || minLon == null || maxLon == null) {
            return true
        }
        return lat in minLat..maxLat && lon in minLon..maxLon
    }

    private fun Location.toDto(): LocationDto = LocationDto(
        stopId = stopId,
        name = name,
        lat = lat,
        lon = lon,
    )

    private fun Location.toSuggestionDto(): LocationSuggestionDto = LocationSuggestionDto(
        stopId = stopId,
        name = name,
        lat = lat,
        lon = lon,
    )
}
