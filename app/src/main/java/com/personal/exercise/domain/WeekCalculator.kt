package com.personal.exercise.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

const val FREE_EARN_YUAN = 200

object WeekCalculator {
    fun statsForWeek(workouts: List<Workout>, weekStart: LocalDate): WeekStats {
        val start = weekStart.weekStartMonday()
        val end = start.plusWeeks(1)
        val inWeek = workouts.filter { it.date >= start && it.date < end }
        return aggregate(start, inWeek)
    }

    fun weeklySeries(
        workouts: List<Workout>,
        endWeekStart: LocalDate,
        weeks: Int,
    ): List<WeekStats> {
        val last = endWeekStart.weekStartMonday()
        val first = last.minusWeeks((weeks - 1).toLong())
        return (0 until weeks).map { offset ->
            val week = first.plusWeeks(offset.toLong())
            statsForWeek(workouts, week)
        }
    }

    fun monthEarnYuan(workouts: List<Workout>, month: YearMonth): Int =
        workouts.count { it.isFree && YearMonth.from(it.date) == month } * FREE_EARN_YUAN

    fun totalEarnYuan(workouts: List<Workout>): Int =
        workouts.count { it.isFree } * FREE_EARN_YUAN

    fun statsForMonth(workouts: List<Workout>, month: YearMonth): MonthStats {
        val inMonth = workouts.filter { YearMonth.from(it.date) == month }
        return MonthStats(
            year = month.year,
            month = month.monthValue,
            rawAerobic = inMonth.filter { it.category == Category.AEROBIC }.sumOf { it.rawMinutes },
            rawStrength = inMonth.filter { it.category == Category.STRENGTH }.sumOf { it.rawMinutes },
            balanceSessions = inMonth.count { it.category == Category.BALANCE },
        )
    }

    fun categoriesOn(workouts: List<Workout>, date: LocalDate): Set<Category> =
        workouts.filter { it.date == date }.map { it.category }.toSet()

    /** Monday-first grid rows a month occupies: 4, 5, or 6. */
    fun weekRowsForMonth(month: YearMonth): Int {
        val shift = (month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        return (shift + month.lengthOfMonth() + 6) / 7
    }

    fun currentTargets(settings: AppSettings): Pair<Int, Int> {
        return if (settings.targetMode == TargetMode.FIXED) {
            settings.fixedAerobic to settings.fixedStrength
        } else {
            settings.stageTargets.aerobic(settings.plan.stage) to
                settings.stageTargets.strength(settings.plan.stage)
        }
    }

    private fun aggregate(weekStart: LocalDate, workouts: List<Workout>): WeekStats {
        var aerobicCredited = 0
        var strengthMinutes = 0
        var balanceSessions = 0
        var rawAerobic = 0
        var rawStrength = 0
        var rawBalance = 0
        for (w in workouts) {
            when (w.category) {
                Category.AEROBIC -> {
                    aerobicCredited += w.creditedMinutes
                    rawAerobic += w.rawMinutes
                }
                Category.STRENGTH -> {
                    strengthMinutes += w.durationMinutes
                    rawStrength += w.rawMinutes
                }
                Category.BALANCE -> {
                    balanceSessions += 1
                    rawBalance += w.rawMinutes
                }
            }
        }
        return WeekStats(
            weekStart = weekStart,
            aerobicCredited = aerobicCredited,
            strengthMinutes = strengthMinutes,
            balanceSessions = balanceSessions,
            rawAerobic = rawAerobic,
            rawStrength = rawStrength,
            rawBalance = rawBalance,
        )
    }
}
