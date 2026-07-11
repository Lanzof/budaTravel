package io.lanzof.ingestor.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import io.lanzof.core.entity.Connection
import io.lanzof.core.entity.GtfsShapePoint as ShapePointEntity
import io.lanzof.core.entity.Location
import io.lanzof.core.repo.ConnectionRepo
import io.lanzof.core.repo.GtfsShapePointRepo
import io.lanzof.core.repo.LocationRepo
import io.lanzof.ingestor.gtfs.GtfsShapePoint
import io.lanzof.ingestor.gtfs.GtfsStop
import io.lanzof.ingestor.gtfs.GtfsStopTime
import io.lanzof.ingestor.gtfs.GtfsTrip
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
    private val connectionRepo: ConnectionRepo,
    private val shapePointRepo: GtfsShapePointRepo,
) {
    private val logger = LoggerFactory.getLogger(GtfsService::class.java)
    private val csvMapper = CsvMapper().apply {
        findAndRegisterModules()
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
    private val gtfsDateFormatter = DateTimeFormatter.BASIC_ISO_DATE

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

        val iterator: MappingIterator<GtfsStop> =
            csvMapper.readerFor(GtfsStop::class.java).with(schema).readValues(inputStream)

        var count = 0
        iterator.forEach { gtfsStop ->
            val location = Location(
                stopId = gtfsStop.stop_id,
                name = gtfsStop.stop_name,
                lat = gtfsStop.stop_lat,
                lon = gtfsStop.stop_lon,
            )
            locationsToSave.add(location)
            count++

            if (count % BATCH_SIZE == 0) {
                locationRepo.saveAll(locationsToSave)
                locationsToSave.clear()
                logger.info("Processed $count stops...")
            }
        }

        if (locationsToSave.isNotEmpty()) {
            locationRepo.saveAll(locationsToSave)
        }

        zipFile.close()
        logger.info("Import finished! Total stops: $count")
    }

    fun importShapesFromZip(zipFilePath: String) {
        logger.info("Starting Shapes import from: $zipFilePath")

        val file = File(zipFilePath)
        if (!file.exists()) {
            logger.error("File not found: $zipFilePath")
            return
        }

        ZipFile(file).use { zipFile ->
            val shapesEntry = zipFile.getEntry("shapes.txt")
                ?: run {
                    logger.warn("shapes.txt not found; route geometry will fall back to stop-to-stop lines")
                    return
                }

            val schema = CsvSchema.emptySchema().withHeader()
            val shapePointsToSave = mutableListOf<ShapePointEntity>()
            var count = 0

            csvMapper
                .readerFor(GtfsShapePoint::class.java)
                .with(schema)
                .readValues<GtfsShapePoint>(zipFile.getInputStream(shapesEntry))
                .asSequence()
                .forEach { point ->
                    shapePointsToSave.add(
                        ShapePointEntity(
                            shapeId = point.shape_id,
                            shapePtSequence = point.shape_pt_sequence,
                            lat = point.shape_pt_lat,
                            lon = point.shape_pt_lon,
                            shapeDistTraveled = point.shape_dist_traveled,
                        )
                    )
                    count++

                    if (count % BATCH_SIZE == 0) {
                        shapePointRepo.saveAll(shapePointsToSave)
                        shapePointsToSave.clear()
                        logger.info("Processed $count shape points...")
                    }
                }

            if (shapePointsToSave.isNotEmpty()) {
                shapePointRepo.saveAll(shapePointsToSave)
            }

            logger.info("Import Shapes finished! Total shape points: $count")
        }
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
            val serviceDate = resolveServiceDate(zipFile)
            val tripMetadataById = readTripMetadata(zipFile)
            logger.info("Resolved GTFS service date: $serviceDate")
            logger.info("Loaded ${tripMetadataById.size} GTFS trips with metadata")

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
                .asSequence()
                .forEach { stopTime ->
                    if (stopTime.trip_id != currentTripId) {
                        if (tripBuffer.isNotEmpty()) {
                            processTrip(
                                stops = tripBuffer,
                                carrier = carrierName,
                                serviceDate = serviceDate,
                                locationMap = locationMap,
                                tripMetadata = tripMetadataById[currentTripId],
                                buffer = connectionsToSave,
                            )
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

            if (tripBuffer.isNotEmpty()) {
                processTrip(
                    stops = tripBuffer,
                    carrier = carrierName,
                    serviceDate = serviceDate,
                    locationMap = locationMap,
                    tripMetadata = tripMetadataById[currentTripId],
                    buffer = connectionsToSave,
                )
            }

            if (connectionsToSave.isNotEmpty()) {
                connectionRepo.saveAll(connectionsToSave)
            }
        }

        logger.info("Import StopTimes finished!")
    }

    private fun processTrip(
        stops: List<GtfsStopTime>,
        carrier: String,
        serviceDate: LocalDate,
        locationMap: Map<String, Location>,
        tripMetadata: TripMetadata?,
        buffer: MutableList<Connection>,
    ) {
        val sortedStops = stops.sortedBy { it.stop_sequence }

        for (i in 0 until sortedStops.size - 1) {
            val from = sortedStops[i]
            val to = sortedStops[i + 1]

            val fromLoc = locationMap[from.stop_id]
            val toLoc = locationMap[to.stop_id]

            if (fromLoc != null && toLoc != null && fromLoc.id != toLoc.id) {
                val conn = Connection(
                    fromLocation = fromLoc,
                    toLocation = toLoc,
                    departureTime = parseGtfsTime(from.departure_time, serviceDate),
                    arrivalTime = parseGtfsTime(to.arrival_time, serviceDate),
                    price = BigDecimal.ZERO,
                    carrier = carrier,
                    type = "PUBLIC_TRANSPORT",
                    tripId = from.trip_id,
                    routeId = tripMetadata?.routeId,
                    shapeId = tripMetadata?.shapeId,
                    fromStopSequence = from.stop_sequence,
                    toStopSequence = to.stop_sequence,
                    fromShapeDistTraveled = from.shape_dist_traveled,
                    toShapeDistTraveled = to.shape_dist_traveled,
                )
                buffer.add(conn)
            }
        }
    }

    private fun readTripMetadata(zipFile: ZipFile): Map<String, TripMetadata> {
        val tripsEntry = zipFile.getEntry("trips.txt")
            ?: run {
                logger.warn("trips.txt not found; route geometry will not have trip/shape metadata")
                return emptyMap()
            }

        val schema = CsvSchema.emptySchema().withHeader()
        return csvMapper
            .readerFor(GtfsTrip::class.java)
            .with(schema)
            .readValues<GtfsTrip>(zipFile.getInputStream(tripsEntry))
            .asSequence()
            .associate { trip ->
                trip.trip_id to TripMetadata(
                    tripId = trip.trip_id,
                    routeId = trip.route_id,
                    shapeId = trip.shape_id,
                )
            }
    }

    // MVP/demo simplification: budapest-mini.zip contains one service_id and one active
    // calendar_dates.txt date, so a single feed-level service date is enough for now.
    // Full GTFS support should resolve the date per trip service_id and handle
    // calendar.txt plus calendar_dates exception_type=1/2 semantics.
    private fun resolveServiceDate(zipFile: ZipFile): LocalDate {
        val calendarDatesEntry = zipFile.getEntry("calendar_dates.txt")
        if (calendarDatesEntry != null) {
            zipFile.getInputStream(calendarDatesEntry).bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val fields = line.split(',')
                    if (fields.size >= 2 && fields[1].isNotBlank()) {
                        return LocalDate.parse(fields[1].trim(), gtfsDateFormatter)
                    }
                }
            }
        }

        val feedInfoEntry = zipFile.getEntry("feed_info.txt")
        if (feedInfoEntry != null) {
            zipFile.getInputStream(feedInfoEntry).bufferedReader().useLines { lines ->
                val iterator = lines.iterator()
                if (iterator.hasNext()) {
                    val header = iterator.next().split(',')
                    val startDateIndex = header.indexOf("feed_start_date")
                    if (startDateIndex >= 0 && iterator.hasNext()) {
                        val values = iterator.next().split(',')
                        val feedStartDate = values.getOrNull(startDateIndex)?.trim()
                        if (!feedStartDate.isNullOrBlank()) {
                            return LocalDate.parse(feedStartDate, gtfsDateFormatter)
                        }
                    }
                }
            }
        }

        logger.warn("GTFS service date not found in calendar_dates.txt or feed_info.txt; falling back to current date")
        return LocalDate.now(ZoneId.of("Europe/Budapest"))
    }

    private fun parseGtfsTime(timeStr: String, serviceDate: LocalDate): OffsetDateTime {
        val parts = timeStr.split(":")
        var hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        val seconds = parts[2].toInt()

        val daysToAdd = hours / 24
        hours %= 24

        val time = LocalTime.of(hours, minutes, seconds)
        return serviceDate
            .atTime(time)
            .atZone(ZoneId.of("Europe/Budapest"))
            .toOffsetDateTime()
            .plusDays(daysToAdd.toLong())
    }

    private data class TripMetadata(
        val tripId: String,
        val routeId: String,
        val shapeId: String?,
    )

    companion object {
        private const val BATCH_SIZE = 1_000
    }
}
