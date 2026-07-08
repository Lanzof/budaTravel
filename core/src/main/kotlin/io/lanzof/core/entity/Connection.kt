package io.lanzof.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "connections")
data class Connection(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_location_id")
    var fromLocation: Location,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_location_id")
    var toLocation: Location,

    val departureTime: OffsetDateTime,
    val arrivalTime: OffsetDateTime,

    val price: BigDecimal,
    val carrier: String,
    val type: String,

    @Column(name = "trip_id")
    val tripId: String? = null,

    @Column(name = "route_id")
    val routeId: String? = null,

    @Column(name = "shape_id")
    val shapeId: String? = null,

    @Column(name = "from_stop_sequence")
    val fromStopSequence: Int? = null,

    @Column(name = "to_stop_sequence")
    val toStopSequence: Int? = null,

    @Column(name = "from_shape_dist_traveled")
    val fromShapeDistTraveled: Double? = null,

    @Column(name = "to_shape_dist_traveled")
    val toShapeDistTraveled: Double? = null,
)
