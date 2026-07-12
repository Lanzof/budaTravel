package io.lanzof.ingestor.gtfs.archive

import java.io.InputStream

interface GtfsArchive {
    val description: String

    fun openEntry(name: String): InputStream?
}
