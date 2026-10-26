package com.personal.exercise.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class WeekCalculatorTest {

    private val monday = LocalDate.of(2026, 10, 12) // Monday

    @Test
    fun weekStartsMonday() {
        val wednesday = LocalDate.of(2026, 10, 14)
        assertEquals(DayOfWeek.MONDAY, wednesday.weekStartMonday().dayOfWeek)
        assertEquals(monday, wednesday.weekStartMonday())
        assertEquals(monday, monday.weekStartMonday())
        assertEquals(monday, LocalDate.of(2026, 10, 18).weekStartMonday())
    }

    @Test
    fun aerobicLowCreditsZero() {
        val w = workout(Category.AEROBIC, 40, Intensity.LOW, monday)
        val stats = WeekCalculator.statsForWeek(listOf(w), monday)
        assertEquals(0, stats.aerobicCredited)
        assertEquals(40, stats.rawAerobic)
        assertEquals(40, stats.rawTotal)
    }

    @Test
    fun aerobicMidCreditsOneX() {
        val w = workout(Category.AEROBIC, 30, Intensity.MID, monday)
        val stats = WeekCalculator.statsForWeek(listOf(w), monday)
        assertEquals(30, stats.aerobicCredited)
    }

    @Test
    fun aerobicHighCreditsTwoX() {
        val w = workout(Category.AEROBIC, 25, Intensity.HIGH, monday.plusDays(2))
        val stats = WeekCalculator.statsForWeek(listOf(w), monday)
        assertEquals(50, stats.aerobicCredited)
        assertEquals(25, stats.rawAerobic)
    }

    @Test
    fun strengthUsesRawDuration() {
        val w = workout(Category.STRENGTH, 25, null, monday)
        val stats = WeekCalculator.statsForWeek(listOf(w), monday)
        assertEquals(25, stats.strengthMinutes)
        assertEquals(25, stats.rawStrength)
        assertEquals(0, stats.aerobicCredited)
    }

    @Test
    fun balanceCountsSessionsNotMinutesForGoal() {
        val sessions = listOf(
            workout(Category.BALANCE, 30, null, monday),
            workout(Category.BALANCE, 20, null, monday.plusDays(3)),
        )
        val stats = WeekCalculator.statsForWeek(sessions, monday)
        assertEquals(2, stats.balanceSessions)
        assertEquals(50, stats.rawBalance)
        assertEquals(0, stats.aerobicCredited)
        assertEquals(0, stats.strengthMinutes)
    }

    @Test
    fun weekMetRequiresBothAerobicAndStrength() {
        val aerobic = workout(Category.AEROBIC, 80, Intensity.MID, monday)
        val strength = workout(Category.STRENGTH, 50, null, monday)
        assertFalse(WeekCalculator.statsForWeek(listOf(aerobic), monday).isMet(80, 50))
        assertFalse(WeekCalculator.statsForWeek(listOf(strength), monday).isMet(80, 50))
        assertTrue(WeekCalculator.statsForWeek(listOf(aerobic, strength), monday).isMet(80, 50))
    }

    @Test
    fun balanceDoesNotAffectMet() {
        val aerobic = workout(Category.AEROBIC, 80, Intensity.MID, monday)
        val strength = workout(Category.STRENGTH, 50, null, monday)
        val noBalance = WeekCalculator.statsForWeek(listOf(aerobic, strength), monday)
        val withBalance = WeekCalculator.statsForWeek(
            listOf(aerobic, strength, workout(Category.BALANCE, 40, null, monday)),
            monday,
        )
        assertTrue(noBalance.isMet(80, 50))
        assertTrue(withBalance.isMet(80, 50))
        assertEquals(noBalance.aerobicCredited, withBalance.aerobicCredited)
        assertEquals(noBalance.strengthMinutes, withBalance.strengthMinutes)
    }

    @Test
    fun SundayBelongsToCurrentWeekNotNext() {
        val sunday = LocalDate.of(2026, 10, 18)
        val nextMonday = LocalDate.of(2026, 10, 19)
        val w = workout(Category.STRENGTH, 20, null, sunday)
        assertEquals(20, WeekCalculator.statsForWeek(listOf(w), monday).strengthMinutes)
        assertEquals(0, WeekCalculator.statsForWeek(listOf(w), nextMonday).strengthMinutes)
    }

    @Test
    fun aggregatesOnlyTheRequestedWeek() {
        val thisWeek = workout(Category.STRENGTH, 20, null, monday)
        val lastWeek = workout(Category.STRENGTH, 40, null, monday.minusWeeks(1))
        val stats = WeekCalculator.statsForWeek(listOf(thisWeek, lastWeek), monday)
        assertEquals(20, stats.strengthMinutes)
    }

    @Test
    fun weeklySeriesIsOldestToNewest() {
        val w1 = workout(Category.STRENGTH, 10, null, monday.minusWeeks(2))
        val w2 = workout(Category.STRENGTH, 30, null, monday)
        val series = WeekCalculator.weeklySeries(listOf(w1, w2), monday, 3)
        assertEquals(3, series.size)
        assertEquals(monday.minusWeeks(2), series[0].weekStart)
        assertEquals(10, series[0].rawTotal)
        assertEquals(0, series[1].rawTotal)
        assertEquals(30, series[2].rawTotal)
    }

    @Test
    fun currentTargetsFollowMode() {
        val plan = AppSettings(
            targetMode = TargetMode.PLAN,
            plan = PlanState(stage = Stage.STAGE_2),
        )
        val fixed = AppSettings(
            targetMode = TargetMode.FIXED,
            fixedAerobic = 90,
            fixedStrength = 55,
            plan = PlanState(stage = Stage.STAGE_2),
        )
        assertEquals(80 to 50, WeekCalculator.currentTargets(plan))
        assertEquals(90 to 55, WeekCalculator.currentTargets(fixed))
    }

    @Test
    fun currentTargetsUseConfiguredStageMinutes() {
        val settings = AppSettings(
            targetMode = TargetMode.PLAN,
            plan = PlanState(stage = Stage.STAGE_2),
            stageTargets = StageTargets(stage2Aerobic = 70, stage2Strength = 45),
        )
        assertEquals(70 to 45, WeekCalculator.currentTargets(settings))
        val maintain = settings.copy(plan = PlanState(stage = Stage.MAINTAIN))
        assertEquals(100 to 60, WeekCalculator.currentTargets(maintain))
        val maintainCustom = maintain.copy(
            stageTargets = settings.stageTargets.copy(stage3Aerobic = 110, stage3Strength = 70),
        )
        assertEquals(110 to 70, WeekCalculator.currentTargets(maintainCustom))
    }

    @Test
    fun monthStatsUseRawMinutesAndBalanceSessions() {
        val ym = YearMonth.of(2026, 10)
        val workouts = listOf(
            workout(Category.AEROBIC, 40, Intensity.HIGH, LocalDate.of(2026, 10, 2)),
            workout(Category.AEROBIC, 20, Intensity.LOW, LocalDate.of(2026, 10, 5)),
            workout(Category.STRENGTH, 25, null, LocalDate.of(2026, 10, 13)),
            workout(Category.STRENGTH, 30, null, LocalDate.of(2026, 10, 14)),
            workout(Category.BALANCE, 45, null, LocalDate.of(2026, 10, 7)),
            workout(Category.BALANCE, 20, null, LocalDate.of(2026, 10, 21)),
            workout(Category.AEROBIC, 100, Intensity.MID, LocalDate.of(2026, 9, 30)),
            workout(Category.STRENGTH, 50, null, LocalDate.of(2026, 11, 1)),
        )
        val stats = WeekCalculator.statsForMonth(workouts, ym)
        assertEquals(2026, stats.year)
        assertEquals(10, stats.month)
        assertEquals(60, stats.rawAerobic)
        assertEquals(55, stats.rawStrength)
        assertEquals(2, stats.balanceSessions)
    }

    @Test
    fun categoriesOnReturnsUniqueCategoriesForDate() {
        val date = LocalDate.of(2026, 10, 13)
        val workouts = listOf(
            workout(Category.STRENGTH, 25, null, date),
            workout(Category.BALANCE, 30, null, date),
            workout(Category.STRENGTH, 10, null, date),
            workout(Category.AEROBIC, 20, Intensity.MID, date.plusDays(1)),
        )
        assertEquals(
            setOf(Category.STRENGTH, Category.BALANCE),
            WeekCalculator.categoriesOn(workouts, date),
        )
        assertEquals(setOf(Category.AEROBIC), WeekCalculator.categoriesOn(workouts, date.plusDays(1)))
        assertEquals(emptySet<Category>(), WeekCalculator.categoriesOn(workouts, date.plusDays(2)))
    }

    @Test
    fun weekRowsForMonthIsFourFiveOrSix() {
        assertEquals(4, WeekCalculator.weekRowsForMonth(YearMonth.of(2021, 2)))
        assertEquals(5, WeekCalculator.weekRowsForMonth(YearMonth.of(2026, 2)))
        assertEquals(5, WeekCalculator.weekRowsForMonth(YearMonth.of(2026, 10)))
        assertEquals(6, WeekCalculator.weekRowsForMonth(YearMonth.of(2026, 3)))
        assertEquals(6, WeekCalculator.weekRowsForMonth(YearMonth.of(2026, 8)))
    }

    private fun workout(
        category: Category,
        minutes: Int,
        intensity: Intensity?,
        date: LocalDate,
    ) = Workout(
        category = category,
        activityName = "测试",
        durationMinutes = minutes,
        intensity = intensity,
        date = date,
        note = null,
    )
}
