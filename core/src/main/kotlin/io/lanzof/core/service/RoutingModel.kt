package io.lanzof.core.service

import io.lanzof.core.entity.Connection
import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime

data class RoutingPath(
    val segments: List<Connection>,
    val requestedDepartureDateTime: OffsetDateTime,
) {
    val totalDuration: Duration
        get() = segments
            .map { normalizedSegmentDuration(it.departureTime, it.arrivalTime) }
            .fold(Duration.ZERO, Duration::plus)

    val totalPrice: BigDecimal
        get() = segments.fold(BigDecimal.ZERO) { acc, segment -> acc + segment.price }

    companion object {
        fun normalizedSegmentDuration(departureTime: OffsetDateTime, arrivalTime: OffsetDateTime): Duration {
            val raw = Duration.between(departureTime, arrivalTime)
            return if (raw == Duration.ZERO) Duration.ofMinutes(1) else raw
        }
    }
}
