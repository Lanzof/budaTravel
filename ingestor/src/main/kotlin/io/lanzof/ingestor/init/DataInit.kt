package io.lanzof.ingestor.init

import io.lanzof.core.entity.ImportStatus
import io.lanzof.core.service.ImportArchiveMetadata
import io.lanzof.core.service.ImportStatusService
import io.lanzof.ingestor.gtfs.archive.GtfsArchive
import io.lanzof.ingestor.gtfs.archive.GtfsArchiveProvider
import io.lanzof.ingestor.service.GtfsService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataInit(
    private val gtfsService: GtfsService,
    private val importStatusService: ImportStatusService,
    private val gtfsArchiveProvider: GtfsArchiveProvider,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataInit::class.java)

    override fun run(vararg args: String?) {
        val archive = gtfsArchiveProvider.getArchive()
        val currentReadiness = importStatusService.getReadiness(archive.datasetName)

        if (currentReadiness.ready) {
            val completed = importStatusService.latestCompleted(archive.datasetName)
            if (completed != null && archive.isNewerThan(completed)) {
                logger.warn(
                    "GTFS archive source={} dataset={} has changed, but automatic DB rebuild is not implemented yet. " +
                        "Keeping current imported data. New archive metadata: etag={}, lastModified={}, contentLength={}",
                    archive.source,
                    archive.datasetName,
                    archive.metadata?.etag,
                    archive.metadata?.lastModified,
                    archive.metadata?.contentLength,
                )
            } else {
                logger.info(
                    "Skipping GTFS import: dataset={} source={} is already ready, locations={}, connections={}",
                    currentReadiness.datasetName,
                    archive.source,
                    currentReadiness.locationsCount,
                    currentReadiness.connectionsCount,
                )
            }
            return
        }

        if (currentReadiness.locationsCount > 0 || currentReadiness.connectionsCount > 0) {
            error(
                "Database already contains GTFS data, but dataset='${archive.datasetName}' is not ready. " +
                    "Reset the database volume before switching GTFS sources. " +
                    "Automatic import generation switching is not implemented yet."
            )
        }

        logger.info("Starting GTFS import for dataset={} source={}...", archive.datasetName, archive.source)
        importStatusService.markRunning(
            datasetName = archive.datasetName,
            source = archive.source,
            archiveMetadata = archive.metadata.toImportMetadata(),
        )

        try {
            logger.info("Using GTFS archive: {}", archive.description)

            gtfsService.importStopsFromArchive(archive)
            gtfsService.importShapesFromArchive(archive)
            gtfsService.importStopTimesFromArchive(archive, "BKK")

            val completed = importStatusService.markCompleted(
                datasetName = archive.datasetName,
                source = archive.source,
                archiveMetadata = archive.metadata.toImportMetadata(),
            )
            logger.info(
                "GTFS import completed: dataset={}, source={}, locations={}, connections={}",
                completed.datasetName,
                completed.source,
                completed.locationsCount,
                completed.connectionsCount,
            )
        } catch (e: Exception) {
            importStatusService.markFailed(datasetName = archive.datasetName, source = archive.source, error = e)
            logger.error("GTFS import failed", e)
            throw e
        }
    }
}

private fun GtfsArchive.isNewerThan(importStatus: ImportStatus): Boolean {
    val metadata = metadata ?: return false
    return (metadata.etag != null && importStatus.archiveEtag != null && metadata.etag != importStatus.archiveEtag) ||
        (metadata.lastModified != null && importStatus.archiveLastModified != null && metadata.lastModified != importStatus.archiveLastModified) ||
        (metadata.contentLength != null && importStatus.archiveContentLength != null && metadata.contentLength != importStatus.archiveContentLength)
}

private fun io.lanzof.ingestor.gtfs.archive.GtfsArchiveMetadata?.toImportMetadata(): ImportArchiveMetadata? = this?.let {
    ImportArchiveMetadata(
        etag = etag,
        lastModified = lastModified,
        contentLength = contentLength,
        downloadedAt = downloadedAt,
    )
}
