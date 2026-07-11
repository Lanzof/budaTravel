package io.lanzof.ingestor.gtfs

data class GtfsStop(
    val stop_id: String,
    val stop_name: String,
    val stop_lat: Double,
    val stop_lon: Double
)

data class GtfsTrip(
    val trip_id: String,
    val route_id: String,
    val service_id: String,
    val shape_id: String? = null,
)

data class GtfsStopTime(
    val trip_id: String,
    val stop_id: String,
    val stop_sequence: Int,
    val arrival_time: String,
    val departure_time: String,
    val shape_dist_traveled: Double? = null,
)

data class GtfsShapePoint(
    val shape_id: String,
    val shape_pt_sequence: Int,
    val shape_pt_lat: Double,
    val shape_pt_lon: Double,
    val shape_dist_traveled: Double? = null,
)
