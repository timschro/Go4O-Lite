package com.go4o.lite.data.model

data class SiPunchData(
    val code: Int,
    val timestampMs: Long
)

data class SiCardResult(
    val siNumber: String,
    val startTime: Long,
    val finishTime: Long,
    val checkTime: Long,
    val punches: List<SiPunchData>,
    val cardSeries: String
) {
    companion object {
        const val NO_TIME = -1L
    }
}
