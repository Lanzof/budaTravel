package io.lanzof.ingestor.init

import io.lanzof.core.repo.ConnectionRepo
import io.lanzof.core.repo.LocationRepo
import io.lanzof.core.service.ImportStatusService
import io.lanzof.ingestor.service.GtfsService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class DataInit(
    private val locationRepo: LocationRepo,
    private val connectionRepo: ConnectionRepo,
    private val gtfsService: GtfsService,
    private val importStatusService: ImportStatusService,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataInit::class.java)

    override fun run(vararg args: String?) {
        logger.info("Starting GTFS import for {}...", ImportStatusService.DEMO_DATASET_NAME)
        importStatusService.markRunning()

        try {
            connectionRepo.deleteAll()
            locationRepo.deleteAll()

            val zip = ClassPathResource("gtfs/budapest-mini.zip")
            gtfsService.importStopsFromZip(zip.file.absolutePath)
            gtfsService.importStopTimesFromZip(zip.file.absolutePath, "BKK")

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
