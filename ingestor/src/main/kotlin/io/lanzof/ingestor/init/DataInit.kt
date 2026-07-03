package io.lanzof.ingestor.init

import io.lanzof.core.repo.ConnectionRepo
import io.lanzof.core.repo.LocationRepo
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import io.lanzof.ingestor.service.GtfsService
import org.springframework.core.io.ClassPathResource

@Component
class DataInit(
    private val locationRepo: LocationRepo,
    private val connectionRepo: ConnectionRepo,
    private val gtfsService: GtfsService
): CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataInit::class.java)

    override fun run(vararg args: String?) {
        logger.info("Starting GTFS Import...")

        // Пропустим очистку БД, если не хочешь терять данные при каждом запуске,
        // но для тестов лучше оставить.
        locationRepo.deleteAll()
        connectionRepo.deleteAll()

        try {
            // Укажи путь к твоему первому архиву
            // Лучше использовать абсолютный путь или положить файлы в resources
            val zip = ClassPathResource("gtfs/budapest-mini.zip")
            gtfsService.importStopsFromZip(zip.file.absolutePath)

            // 2. Импорт расписания
            gtfsService.importStopTimesFromZip(zip.file.absolutePath, "BKK")
        } catch (e: Exception) {
            logger.error("Import failed", e)
        }
    }
}