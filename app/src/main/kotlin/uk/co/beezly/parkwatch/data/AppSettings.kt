package uk.co.beezly.parkwatch.data

data class AppSettings(
    val carDeviceMac: String = "",
    val carDeviceName: String = "",
    val timerDurationMinutes: Int = 90,
    val locationCheckEnabled: Boolean = true,
    val zoneLat: Double = 53.6458,
    val zoneLng: Double = -1.7850,
    val zoneRadiusMetres: Int = 600,
    // Restriction hours stored as "HH:MM"
    val weekdayStartTime: String = "08:00",
    val weekdayEndTime: String = "18:00",
    val sundayStartTime: String = "12:00",
    val sundayEndTime: String = "18:00"
)
