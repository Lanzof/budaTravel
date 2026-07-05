package io.lanzof.core.repo

import io.lanzof.core.entity.ImportStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ImportStatusRepo : JpaRepository<ImportStatus, UUID> {
    fun findFirstByDatasetNameOrderByStartedAtDesc(datasetName: String): ImportStatus?
}
