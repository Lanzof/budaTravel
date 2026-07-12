package io.lanzof.ingestor.service

import com.fasterxml.jackson.databind.DeserializationFeature
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
import io.lanzof.ingestor.gtfs.archive.GtfsArchive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    fun importStopsFromArchive(archive: GtfsArchive) {
        logger.info("Starting stops import from: {}", archive.description)

        val schema = CsvSchema.emptySchema().withHeader()
        val locationsToSave = mutableListOf<Location>()
        var count = 0

        archive.openRequiredEntry("stops.txt").use { inputStream ->
            csvMapper
                .readerFor(GtfsStop::class.java)
                .with(schema)
                .readValues<GtfsStop>(inputStream)
                .use { iterator ->
                    iterator.forEach { gtfsStop ->
                        locationsToSave.add(
                            Location(
                                stopId = gtfsStop.stop_id,
                                name = gtfsStop.stop_name,
                                lat = gtfsStop.stop_lat,
                                lon = gtfsStop.stop_lon,
                            )
                        )
                        count++

                        if (count % BATCH_SIZE == 0) {
                            locationRepo.saveAll(locationsToSave)
                            locationsToSave.clear()
                            logger.info("Processed {} stops...", count)
                        }
                    }
                }
        }

        if (locationsToSave.isNotEmpty()) {
            locationRepo.saveAll(locationsToSave)
        }

        logger.info("Import stops finished! Total stops: {}", count)
    }

    fun importShapesFromArchive(archive: GtfsArchive) {
        logger.info("Starting shapes import from: {}", archive.description)

        val inputStream = archive.openEntry("shapes.txt")
            ?: run {
                logger.warn("shapes.txt not found; route geometry will fall back to stop-to-stop lines")
                return
            }

        val schema = CsvSchema.emptySchema().withHeader()
        val shapePointsToSave = mutableListOf<ShapePointEntity>()
        var count = 0

        inputStream.use {
            csvMapper
                .readerFor(GtfsShapePoint::class.java)
                .with(schema)
                .readValues<GtfsShapePoint>(it)
                .use { iterator ->
                    iterator.forEach { point ->
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
                            logger.info("Processed {} shape points...", count)
                        }
                    }
                }
        }

        if (shapePointsToSave.isNotEmpty()) {
            shapePointRepo.saveAll(shapePointsToSave)
        }

        logger.info("Import shapes finished! Total shape points: {}", count)
    }

    fun importStopTimesFromArchive(archive: GtfsArchive, carrierName: String) {
        logger.info("Starting stop times import for {} from: {}", carrierName, archive.description)

        val serviceDate = resolveServiceDate(archive)
        val tripMetadataById = readTripMetadata(archive)
        logger.info("Resolved GTFS service date: {}", serviceDate)
        logger.info("Loaded {} GTFS trips with metadata", tripMetadataById.size)

        val locationMap = locationRepo.findAll()
            .associateBy { it.stopId }
        val schema = CsvSchema.emptySchema().withHeader()
        val connectionsToSave = mutableListOf<Connection>()
        var currentTripId: String? = null
        val tripBuffer = mutableListOf<GtfsStopTime>()
        var tripsProcessed = 0

        archive.openRequiredEntry("stop_times.txt").use { inputStream ->
            csvMapper
                .readerFor(GtfsStopTime::class.java)
                .with(schema)
                .readValues<GtfsStopTime>(inputStream)
                .use { iterator ->
                    iterator.forEach { stopTime ->
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
                                    logger.info("Processed {} trips...", tripsProcessed)
                                    connectionRepo.saveAll(connectionsToSave)
                                    connectionsToSave.clear()
                                }
                            }
                            currentTripId = stopTime.trip_id
                        }

                        tripBuffer.add(stopTime)
                    }
                }
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

        logger.info("Import stop times finished!")
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
                buffer.add(
                    Connection(
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
                )
            }
        }
    }

    private fun readTripMetadata(archive: GtfsArchive): Map<String, TripMetadata> {
        val inputStream = archive.openEntry("trips.txt")
            ?: run {
                logger.warn("trips.txt not found; route geometry will not have trip/shape metadata")
                return emptyMap()
            }

        val schema = CsvSchema.emptySchema().withHeader()
        return inputStream.use {
            csvMapper
                .readerFor(GtfsTrip::class.java)
                .with(schema)
                .readValues<GtfsTrip>(it)
                .use { iterator ->
                    iterator.asSequence()
                        .associate { trip ->
                            trip.trip_id to TripMetadata(
                                tripId = trip.trip_id,
                                routeId = trip.route_id,
                                shapeId = trip.shape_id,
                            )
                        }
                }
        }
    }

    // MVP/demo simplification: budapest-mini.zip contains one service_id and one active
    // calendar_dates.txt date, so a single feed-level service date is enough for now.
    // Full GTFS support should resolve the date per trip service_id and handle
    // calendar.txt plus calendar_dates exception_type=1/2 semantics.
    private fun resolveServiceDate(archive: GtfsArchive): LocalDate {
        archive.openEntry("calendar_dates.txt")?.use { inputStream ->
            inputStream.bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val fields = line.split(',')
                    if (fields.size >= 2 && fields[1].isNotBlank()) {
                        return LocalDate.parse(fields[1].trim(), gtfsDateFormatter)
                    }
                }
            }
        }

        archive.openEntry("feed_info.txt")?.use { inputStream ->
            inputStream.bufferedReader().useLines { lines ->
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

    private fun GtfsArchive.openRequiredEntry(name: String): InputStream = openEntry(name)
        ?: throw IllegalArgumentException("$name not found in GTFS archive: $description")

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
