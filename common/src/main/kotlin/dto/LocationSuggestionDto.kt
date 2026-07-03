package io.lanzof.dto

data class LocationSuggestionDto(
    val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)

