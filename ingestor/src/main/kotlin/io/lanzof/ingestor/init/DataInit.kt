package io.lanzof.ingestor.init

import io.lanzof.core.service.ImportStatusService
import io.lanzof.ingestor.service.GtfsService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
class DataInit(
    private val gtfsService: GtfsService,
    private val importStatusService: ImportStatusService,
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
            val zipPath = copyGtfsResourceToTempFile()
            gtfsService.importStopsFromZip(zipPath.toString())
            gtfsService.importShapesFromZip(zipPath.toString())
            gtfsService.importStopTimesFromZip(zipPath.toString(), "BKK")

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

    /**
     * MVP workaround for running the ingestor from a Spring Boot executable jar.
     *
     * Inside `app.jar`, classpath resources are nested and cannot be addressed as regular
     * filesystem paths. `GtfsService` currently accepts only a file path and opens `ZipFile` /
     * `File` internally, so the bundled demo archive is copied to a temporary file first.
     *
     * Later, prefer a `GtfsArchiveProvider` or `Resource` / `InputStream` based import flow to
     * support classpath, mounted, and downloaded archives without this adapter.
     */
    private fun copyGtfsResourceToTempFile(): java.nio.file.Path {
        val resource = ClassPathResource("gtfs/budapest-mini.zip")
        val tempFile = Files.createTempFile("budapest-mini-", ".zip")
        resource.inputStream.use { input ->
            Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
        tempFile.toFile().deleteOnExit()
        return tempFile
    }
}
