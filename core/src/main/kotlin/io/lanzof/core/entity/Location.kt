package io.lanzof.core.entity

import io.lanzof.core.search.LocationSearchNormalizer
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "location")
data class Location(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    val stopId: String,
    val name: String,

    @Column(name = "name_normalized", nullable = false)
    val normalizedName: String = LocationSearchNormalizer.normalize(name),

    val lat: Double,
    val lon: Double,
)
