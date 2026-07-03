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
    val service_id: String
    // Может быть еще trip_headsign (направление), но пока не нужно
)

data class GtfsStopTime(
    val trip_id: String,
    val stop_id: String, // ID станции (должен совпадать с stop_id из stops.txt)
    val stop_sequence: Int, // Порядковый номер остановки (0, 1, 2...)
    val arrival_time: String, // Формат "HH:MM:SS"
    val departure_time: String // Формат "HH:MM:SS"
)
