package io.lanzof.core.repo

import io.lanzof.core.entity.Location
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LocationRepo : JpaRepository<Location, UUID> {

    fun findByNormalizedNameContaining(normalizedName: String): List<Location>
    fun findByStopId(stopId: String): List<Location>

}