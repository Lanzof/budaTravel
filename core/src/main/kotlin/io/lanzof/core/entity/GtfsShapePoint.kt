package io.lanzof.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "gtfs_shape_points")
data class GtfsShapePoint(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "shape_id", nullable = false)
    val shapeId: String,

    @Column(name = "shape_pt_sequence", nullable = false)
    val shapePtSequence: Int,

    @Column(nullable = false)
    val lat: Double,

    @Column(nullable = false)
    val lon: Double,

    @Column(name = "shape_dist_traveled")
    val shapeDistTraveled: Double? = null,
)
