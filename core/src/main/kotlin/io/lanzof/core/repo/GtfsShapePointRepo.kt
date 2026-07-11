package io.lanzof.core.repo

import io.lanzof.core.entity.GtfsShapePoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GtfsShapePointRepo : JpaRepository<GtfsShapePoint, UUID> {
    fun findByShapeIdAndShapeDistTraveledBetweenOrderByShapeDistTraveledAscShapePtSequenceAsc(
        shapeId: String,
        fromDistance: Double,
        toDistance: Double,
    ): List<GtfsShapePoint>
}
