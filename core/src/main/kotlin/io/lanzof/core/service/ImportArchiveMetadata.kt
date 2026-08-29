package io.lanzof.core.service

data class ImportArchiveMetadata(
    val etag: String? = null,
    val lastModified: String? = null,
    val contentLength: Long? = null,
    val downloadedAt: String? = null,
)
