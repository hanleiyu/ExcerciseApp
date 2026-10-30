package com.personal.exercise.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class EarnYuanTest {

    private val october = YearMonth.of(2026, 10)

    @Test
    fun unpaidWorkoutsContributeZero() {
        val workouts = listOf(
            workout(LocalDate.of(2026, 10, 2), isFree = false),
            workout(LocalDate.of(2026, 10, 5), isFree = false),
        )
        assertEquals(0, WeekCalculator.monthEarnYuan(workouts, october))
        assertEquals(0, WeekCalculator.totalEarnYuan(workouts))
    }

    @Test
    fun eachFreeWorkoutAddsHardcoded200() {
        val workouts = listOf(
            workout(LocalDate.of(2026, 10, 2), isFree = true),
            workout(LocalDate.of(2026, 10, 5), isFree = true),
            workout(LocalDate.of(2026, 10, 8), isFree = false),
        )
        assertEquals(400, WeekCalculator.monthEarnYuan(workouts, october))
        assertEquals(400, WeekCalculator.totalEarnYuan(workouts))
        assertEquals(200, FREE_EARN_YUAN)
    }

    @Test
    fun monthEarnFiltersByViewedYearMonth() {
        val workouts = listOf(
            workout(LocalDate.of(2026, 10, 26), isFree = true),
            workout(LocalDate.of(2026, 10, 27), isFree = true),
            workout(LocalDate.of(2026, 9, 30), isFree = true),
            workout(LocalDate.of(2026, 11, 1), isFree = true),
        )
        assertEquals(400, WeekCalculator.monthEarnYuan(workouts, october))
        assertEquals(0, WeekCalculator.monthEarnYuan(workouts, YearMonth.of(2026, 8)))
        assertEquals(200, WeekCalculator.monthEarnYuan(workouts, YearMonth.of(2026, 9)))
    }

    @Test
    fun totalEarnUsesAllHistoryAndIgnoresMonthSwitch() {
        val workouts = listOf(
            workout(LocalDate.of(2026, 10, 26), isFree = true),
            workout(LocalDate.of(2026, 9, 1), isFree = true),
            workout(LocalDate.of(2025, 1, 1), isFree = true),
            workout(LocalDate.of(2026, 10, 1), isFree = false),
        )
        assertEquals(600, WeekCalculator.totalEarnYuan(workouts))
        assertEquals(200, WeekCalculator.monthEarnYuan(workouts, october))
        assertEquals(200, WeekCalculator.monthEarnYuan(workouts, YearMonth.of(2026, 9)))
    }

    @Test
    fun uncheckOrDeleteReducesBothTotals() {
        val kept = listOf(
            workout(LocalDate.of(2026, 10, 2), isFree = true),
            workout(LocalDate.of(2026, 10, 3), isFree = false),
        )
        val withExtraFree = kept + workout(LocalDate.of(2026, 10, 4), isFree = true)
        assertEquals(400, WeekCalculator.monthEarnYuan(withExtraFree, october))
        assertEquals(400, WeekCalculator.totalEarnYuan(withExtraFree))
        assertEquals(200, WeekCalculator.monthEarnYuan(kept, october))
        assertEquals(200, WeekCalculator.totalEarnYuan(kept))
    }

    @Test
    fun emptyHistoryIsZero() {
        assertEquals(0, WeekCalculator.monthEarnYuan(emptyList(), october))
        assertEquals(0, WeekCalculator.totalEarnYuan(emptyList()))
    }

    private fun workout(date: LocalDate, isFree: Boolean) = Workout(
        category = Category.AEROBIC,
        activityName = "测试",
        durationMinutes = 30,
        intensity = Intensity.MID,
        date = date,
        note = null,
        isFree = isFree,
    )
}
