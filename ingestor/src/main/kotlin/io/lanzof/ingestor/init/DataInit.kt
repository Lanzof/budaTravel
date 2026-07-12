package io.lanzof.ingestor.init

import io.lanzof.core.service.ImportStatusService
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
        val currentReadiness = importStatusService.getReadiness()
        if (currentReadiness.ready) {
            logger.info(
                "Skipping GTFS import: dataset={} is already ready, locations={}, connections={}",
                currentReadiness.datasetName,
                currentReadiness.locationsCount,
                currentReadiness.connectionsCount,
            )
            return
        }

        logger.info("Starting GTFS import for {}...", ImportStatusService.DEMO_DATASET_NAME)
        importStatusService.markRunning()

        try {
            val archive = gtfsArchiveProvider.getArchive()
            logger.info("Using GTFS archive: {}", archive.description)

            gtfsService.importStopsFromArchive(archive)
            gtfsService.importShapesFromArchive(archive)
            gtfsService.importStopTimesFromArchive(archive, "BKK")

            val completed = importStatusService.markCompleted()
            logger.info(
                "GTFS import completed: dataset={}, locations={}, connections={}",
                completed.datasetName,
                completed.locationsCount,
                completed.connectionsCount,
            )
        } catch (e: Exception) {
            importStatusService.markFailed(error = e)
            logger.error("GTFS import failed", e)
            throw e
        }
    }
}
