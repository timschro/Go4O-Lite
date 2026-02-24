package com.go4o.lite.domain

import com.go4o.lite.data.model.*
import org.junit.Assert.*
import org.junit.Test

class CourseEvaluatorTest {

    private fun makeCard(
        punches: List<Int>,
        startTime: Long = 36000000,
        finishTime: Long = 39600000
    ): SiCardResult {
        return SiCardResult(
            siNumber = "123456",
            startTime = startTime,
            finishTime = finishTime,
            checkTime = 35000000,
            punches = punches.map { SiPunchData(code = it, timestampMs = 0) },
            cardSeries = "SiCard 8"
        )
    }

    private val course = Course(name = "Short", controls = listOf(31, 32, 33))

    @Test
    fun passWhenAllControlsInOrder() {
        val card = makeCard(listOf(31, 32, 33))
        val result = CourseEvaluator.evaluate(card, listOf(course))

        assertEquals(ResultStatus.PASS, result.status)
        assertEquals(course, result.matchedCourse)
        assertTrue(result.missingControls.isEmpty())
    }

    @Test
    fun passWithExtraPunches() {
        val card = makeCard(listOf(99, 31, 88, 32, 77, 33, 66))
        val result = CourseEvaluator.evaluate(card, listOf(course))

        assertEquals(ResultStatus.PASS, result.status)
        assertTrue(result.missingControls.isEmpty())
    }

    @Test
    fun failWhenMissingControl() {
        val card = makeCard(listOf(31, 33))
        val result = CourseEvaluator.evaluate(card, listOf(course))

        assertEquals(ResultStatus.FAIL, result.status)
        assertEquals(listOf(32), result.missingControls)
    }

    @Test
    fun failWhenControlsOutOfOrder() {
        val card = makeCard(listOf(33, 32, 31))
        val result = CourseEvaluator.evaluate(card, listOf(course))

        assertEquals(ResultStatus.FAIL, result.status)
        assertEquals(listOf(32, 33), result.missingControls)
    }

    @Test
    fun failWhenNoPunches() {
        val card = makeCard(emptyList())
        val result = CourseEvaluator.evaluate(card, listOf(course))

        assertEquals(ResultStatus.FAIL, result.status)
        assertEquals(listOf(31, 32, 33), result.missingControls)
    }

    @Test
    fun noCourseWhenNoCoursesLoaded() {
        val card = makeCard(listOf(31, 32, 33))
        val result = CourseEvaluator.evaluate(card, emptyList())

        assertEquals(ResultStatus.NO_COURSE, result.status)
        assertNull(result.matchedCourse)
    }

    @Test
    fun matchesBestCourseWhenMultiple() {
        val longCourse = Course(name = "Long", controls = listOf(31, 42, 53, 64, 33))
        val card = makeCard(listOf(31, 32, 33))
        val result = CourseEvaluator.evaluate(card, listOf(course, longCourse))

        assertEquals(ResultStatus.PASS, result.status)
        assertEquals("Short", result.matchedCourse?.name)
    }

    @Test
    fun runningTimeCalculation() {
        val card = makeCard(listOf(31, 32, 33), startTime = 36000000, finishTime = 39600000)
        val result = CourseEvaluator.evaluate(card, listOf(course))

        assertEquals(3600000L, result.runningTimeMs)
    }

    @Test
    fun noRunningTimeWhenNoStart() {
        val card = makeCard(listOf(31, 32, 33), startTime = SiCardResult.NO_TIME)
        val result = CourseEvaluator.evaluate(card, listOf(course))

        assertNull(result.runningTimeMs)
    }

    @Test
    fun noRunningTimeWhenNoFinish() {
        val card = makeCard(listOf(31, 32, 33), finishTime = SiCardResult.NO_TIME)
        val result = CourseEvaluator.evaluate(card, listOf(course))

        assertNull(result.runningTimeMs)
    }
}
