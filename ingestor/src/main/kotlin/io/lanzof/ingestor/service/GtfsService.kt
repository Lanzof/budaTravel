package io.lanzof.ingestor.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import io.lanzof.core.entity.Connection
import io.lanzof.core.entity.Location
import io.lanzof.core.repo.ConnectionRepo
import io.lanzof.core.repo.LocationRepo
import io.lanzof.ingestor.gtfs.GtfsStop
import io.lanzof.ingestor.gtfs.GtfsStopTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipFile

@Service
class GtfsService(
    private val locationRepo: LocationRepo,
    private val connectionRepo: ConnectionRepo
) {
    private val logger = LoggerFactory.getLogger(GtfsService::class.java)
    private val csvMapper = CsvMapper().apply {
        // Настройка: первая строка - это заголовок
        findAndRegisterModules()
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
    private val gtfsTimeFormatter = DateTimeFormatter.ofPattern("H:mm:ss")

    fun importStopsFromZip(zipFilePath: String) {
        logger.info("Starting import from: $zipFilePath")

        val path = Paths.get(zipFilePath)
        if (!path.toFile().exists()) {
            logger.error("File not found: $zipFilePath")
            return
        }

        val zipFile = ZipFile(path.toFile())
        val stopsEntry = zipFile.getEntry("stops.txt")

        if (stopsEntry == null) {
            logger.error("stops.txt not found in archive")
            zipFile.close()
            return
        }

        val inputStream: InputStream = zipFile.getInputStream(stopsEntry)
        val schema: CsvSchema = CsvSchema.emptySchema().withHeader()

        val locationsToSave = mutableListOf<Location>()

        // Читаем CSV построчно
        val iterator: MappingIterator<GtfsStop> =
            csvMapper.readerFor(GtfsStop::class.java).with(schema).readValues(inputStream)

        var count = 0
        iterator.forEach { gtfsStop ->
            // Маппим GTFS модель в нашу Entity
            val location = Location(
                stopId = gtfsStop.stop_id,
                name = gtfsStop.stop_name,
                lat = gtfsStop.stop_lat, // Убедись, что в Entity lat/lon Double (или конвертируй)
                lon = gtfsStop.stop_lon
            )
            locationsToSave.add(location)
            count++

            // Пакетная запись каждые 1000 штук, чтобы не забить память
            if (count % 1000 == 0) {
                locationRepo.saveAll(locationsToSave)
                locationsToSave.clear()
                logger.info("Processed $count stops...")
            }
        }

        // Сохраняем остаток
        if (locationsToSave.isNotEmpty()) {
            locationRepo.saveAll(locationsToSave)
        }

        zipFile.close()
        logger.info("Import finished! Total stops: $count")
    }

    fun importStopTimesFromZip(zipFilePath: String, carrierName: String) {
        logger.info("Starting StopTimes import for $carrierName from: $zipFilePath")

        val file = File(zipFilePath)
        if (!file.exists()) {
            logger.error("File not found: $zipFilePath")
            return
        }

        ZipFile(file).use { zipFile ->
            val stopTimesEntry = zipFile.getEntry("stop_times.txt")
                ?: run {
                    logger.error("stop_times.txt not found")
                    return
                }

            // Оптимизация: загружаем все локации в Map ОДИН раз, чтобы не делать N+1 запросов в цикле
            val locationMap = locationRepo.findAll()
                .associateBy { it.stopId }

            val inputStream = zipFile.getInputStream(stopTimesEntry)
            val schema = CsvSchema.emptySchema().withHeader()

            val connectionsToSave = mutableListOf<Connection>()
            var currentTripId: String? = null
            val tripBuffer = mutableListOf<GtfsStopTime>()
            var tripsProcessed = 0

            csvMapper
                .readerFor(GtfsStopTime::class.java)
                .with(schema)
                .readValues<GtfsStopTime>(inputStream)
                .asSequence() // Используем Sequence для ленивой итерации
                .forEach { stopTime ->

                    // Если ID поездки сменился, обрабатываем накопленный буфер
                    if (stopTime.trip_id != currentTripId) {
                        if (tripBuffer.isNotEmpty()) {
                            processTrip(tripBuffer, carrierName, locationMap, connectionsToSave)
                            tripBuffer.clear()

                            tripsProcessed++
                            if (tripsProcessed % 100 == 0) {
                                logger.info("Processed $tripsProcessed trips...")
                                connectionRepo.saveAll(connectionsToSave)
                                connectionsToSave.clear()
                            }
                        }
                        currentTripId = stopTime.trip_id
                    }

                    tripBuffer.add(stopTime)
                }

            // Обрабатываем последнюю поездку в файле
            if (tripBuffer.isNotEmpty()) {
                processTrip(tripBuffer, carrierName, locationMap, connectionsToSave)
            }

            // Сохраняем оставшийся батч
            if (connectionsToSave.isNotEmpty()) {
                connectionRepo.saveAll(connectionsToSave)
            }
        }

        logger.info("Import StopTimes finished!")
    }

    private fun processTrip(
        stops: List<GtfsStopTime>,
        carrier: String,
        locationMap: Map<String, Location>, // Передаем карту вместо репозитория
        buffer: MutableList<Connection>
    ) {
        // Сортируем остановки по порядку (на случай, если в файле они перемешаны)
        val sortedStops = stops.sortedBy { it.stop_sequence }

        // Идем парами: i -> i+1
        for (i in 0 until sortedStops.size - 1) {
            val from = sortedStops[i]
            val to = sortedStops[i + 1]

            // Достаем локации из Map (O(1)), а не из БД (О(N))
            val fromLoc = locationMap[from.stop_id]
            val toLoc = locationMap[to.stop_id]

            if (fromLoc != null && toLoc != null && fromLoc.id != toLoc.id) {
                val conn = Connection(
                    fromLocation = fromLoc,
                    toLocation = toLoc,
                    departureTime = parseGtfsTime(from.departure_time),
                    arrivalTime = parseGtfsTime(to.arrival_time),
                    price = BigDecimal.ZERO, // В GTFS цен нет
                    carrier = carrier,
                    type = "PUBLIC_TRANSPORT"
                )
                buffer.add(conn)
            }
        }
    }

    private fun parseGtfsTime(timeStr: String): OffsetDateTime {
        val parts = timeStr.split(":")
        var hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        val seconds = parts[2].toInt()

        val daysToAdd = hours / 24
        hours %= 24

        val time = LocalTime.of(hours, minutes, seconds)
        return LocalDate.now()
            .atTime(time)
            .atZone(ZoneId.of("Europe/Budapest"))
            .toOffsetDateTime()
            .plusDays(daysToAdd.toLong())
    }
}