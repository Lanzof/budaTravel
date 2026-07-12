package io.lanzof.core.service

import io.lanzof.core.entity.ImportStatusValue
import java.time.OffsetDateTime

data class DataReadiness(
    val datasetName: String,
    val ready: Boolean,
    val status: ImportStatusValue?,
    val startedAt: OffsetDateTime?,
    val completedAt: OffsetDateTime?,
    val locationsCount: Long,
    val connectionsCount: Long,
    val errorMessage: String?,
    val source: String? = null,
    val archiveEtag: String? = null,
    val archiveLastModified: String? = null,
    val archiveContentLength: Long? = null,
)
