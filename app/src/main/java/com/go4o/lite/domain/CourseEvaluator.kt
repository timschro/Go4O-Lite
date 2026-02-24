package com.go4o.lite.domain

import com.go4o.lite.data.model.*

object CourseEvaluator {

    fun evaluate(cardResult: SiCardResult, courses: List<Course>): ReadoutResult {
        if (courses.isEmpty()) {
            return ReadoutResult(
                siCardResult = cardResult,
                matchedCourse = null,
                status = ResultStatus.NO_COURSE,
                runningTimeMs = computeRunningTime(cardResult),
                missingControls = emptyList()
            )
        }

        // Try each course; return the first that passes, otherwise the best fail
        var bestFail: ReadoutResult? = null

        for (course in courses) {
            val missing = findMissingControls(cardResult.punches, course.controls)
            val runningTime = computeRunningTime(cardResult)

            if (missing.isEmpty()) {
                return ReadoutResult(
                    siCardResult = cardResult,
                    matchedCourse = course,
                    status = ResultStatus.PASS,
                    runningTimeMs = runningTime,
                    missingControls = emptyList()
                )
            }

            if (bestFail == null || missing.size < bestFail.missingControls.size) {
                bestFail = ReadoutResult(
                    siCardResult = cardResult,
                    matchedCourse = course,
                    status = ResultStatus.FAIL,
                    runningTimeMs = runningTime,
                    missingControls = missing
                )
            }
        }

        return bestFail!!
    }

    fun findMissingControls(punches: List<SiPunchData>, courseControls: List<Int>): List<Int> {
        val missing = mutableListOf<Int>()
        var punchIndex = 0

        for (control in courseControls) {
            var found = false
            while (punchIndex < punches.size) {
                if (punches[punchIndex].code == control) {
                    punchIndex++
                    found = true
                    break
                }
                punchIndex++
            }
            if (!found) {
                missing.add(control)
            }
        }
        return missing
    }

    fun computeRunningTime(cardResult: SiCardResult): Long? {
        val start = cardResult.startTime
        val finish = cardResult.finishTime
        if (start == SiCardResult.NO_TIME || finish == SiCardResult.NO_TIME) return null
        val diff = finish - start
        return if (diff >= 0) diff else null
    }
}
