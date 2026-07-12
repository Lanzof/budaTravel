package io.lanzof.ingestor.gtfs.archive

import java.io.InputStream

interface GtfsArchive {
    val source: String
    val datasetName: String
    val description: String
    val metadata: GtfsArchiveMetadata?

    fun openEntry(name: String): InputStream?
}

data class GtfsArchiveMetadata(
    val etag: String? = null,
    val lastModified: String? = null,
    val contentLength: Long? = null,
    val downloadedAt: String? = null,
)
