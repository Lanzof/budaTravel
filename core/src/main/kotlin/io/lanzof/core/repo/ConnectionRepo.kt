package io.lanzof.core.repo

import io.lanzof.core.entity.Connection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface ConnectionRepo : JpaRepository<Connection, UUID> {

    @Query("""
        SELECT c FROM Connection c 
        WHERE c.fromLocation.id = :fromId 
            AND c.toLocation.id = :toId 
            AND c.departureTime > :time
    """)
    fun findConnections(
        @Param("fromId") fromId: UUID,
        @Param("toId") toId: UUID,
        @Param("time") time: OffsetDateTime
    ): List<Connection>

    @Query(
        """
        SELECT c FROM Connection c
        WHERE c.fromLocation.id = :fromId
          AND c.departureTime >= :time
          AND UPPER(c.type) IN :types
        ORDER BY c.departureTime ASC
        """
    )
    fun findNextConnections(
        @Param("fromId") fromId: UUID,
        @Param("time") time: OffsetDateTime,
        @Param("types") types: List<String>,
        pageable: Pageable,
    ): List<Connection>
}
