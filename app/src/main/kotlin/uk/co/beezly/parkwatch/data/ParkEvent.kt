package uk.co.beezly.parkwatch.data

data class ParkEvent(
    val id: Long = System.currentTimeMillis(),
    val startTime: Long,
    val endTime: Long? = null,
    val outcome: ParkOutcome = ParkOutcome.ACTIVE
)

enum class ParkOutcome {
    ACTIVE,
    CANCELLED,  // Returned to car before timer expired
    EXPIRED     // Timer ran out
}
