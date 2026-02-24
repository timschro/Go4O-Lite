package com.go4o.lite.data.model

enum class ResultStatus {
    PASS, FAIL, NO_COURSE
}

data class ReadoutResult(
    val siCardResult: SiCardResult,
    val matchedCourse: Course?,
    val status: ResultStatus,
    val runningTimeMs: Long?,
    val missingControls: List<Int>
)
