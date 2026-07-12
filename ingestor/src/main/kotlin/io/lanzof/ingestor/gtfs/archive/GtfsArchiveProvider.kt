package io.lanzof.ingestor.gtfs.archive

interface GtfsArchiveProvider {
    fun getArchive(): GtfsArchive
}
