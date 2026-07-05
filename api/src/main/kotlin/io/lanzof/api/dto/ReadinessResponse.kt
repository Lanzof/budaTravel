package io.lanzof.api.dto

import io.lanzof.core.entity.ImportStatusValue
import java.time.OffsetDateTime

data class ReadinessResponse(
    val datasetName: String,
    val ready: Boolean,
    val status: ImportStatusValue?,
    val startedAt: OffsetDateTime?,
    val completedAt: OffsetDateTime?,
    val locationsCount: Long,
    val connectionsCount: Long,
    val errorMessage: String?,
)
