package com.personal.exercise.domain

import java.time.LocalDate

object PlanEngine {
    const val WEEKS_TO_ADVANCE = 2

    fun evaluate(
        state: PlanState,
        today: LocalDate,
        mode: TargetMode,
        workouts: List<Workout>,
        targets: StageTargets = StageTargets(),
        weeksToAdvance: Int = WEEKS_TO_ADVANCE,
    ): PlanState {
        if (mode != TargetMode.PLAN) return state.copy(pendingAdvanceMessage = null)

        val currentWeekStart = today.weekStartMonday()
        val earliestWorkoutWeek = workouts.minOfOrNull { it.date.weekStartMonday() }
            ?: return state.copy(pendingAdvanceMessage = null)
        val needed = weeksToAdvance.coerceAtLeast(1)

        var week = when (val last = state.lastEvaluatedWeekStart) {
            null -> earliestWorkoutWeek
            else -> last.plusWeeks(1)
        }

        var result = state.copy(pendingAdvanceMessage = null)
        val messages = mutableListOf<String>()
        while (week < currentWeekStart) {
            result = evaluateOneCompletedWeek(result, week, workouts, targets, needed)
            result.pendingAdvanceMessage?.let { messages += it }
            week = week.plusWeeks(1)
        }
        return result.copy(pendingAdvanceMessage = messages.lastOrNull())
    }

    fun resetKeepingHistoryUnevaluated(today: LocalDate): PlanState {
        val lastCompleted = today.weekStartMonday().minusWeeks(1)
        return PlanState(lastEvaluatedWeekStart = lastCompleted)
    }

    fun freezeEvaluationAt(today: LocalDate, current: PlanState): PlanState {
        return current.copy(
            lastEvaluatedWeekStart = today.weekStartMonday().minusWeeks(1),
            pendingAdvanceMessage = null,
        )
    }

    fun upgradeHint(
        state: PlanState,
        mode: TargetMode,
        targets: StageTargets = StageTargets(),
        weeksToAdvance: Int = WEEKS_TO_ADVANCE,
    ): String? {
        if (mode != TargetMode.PLAN) return null
        val next = state.stage.nextOrNull() ?: return null
        val needed = weeksToAdvance.coerceAtLeast(1)
        if (state.consecutiveMetWeeks < needed - 1) return null
        return "本周再达标即可升到${next.label}（有氧 ${targets.aerobic(next)} · 力量 ${targets.strength(next)}）"
    }

    fun stageLine(state: PlanState, mode: TargetMode): String {
        return if (mode == TargetMode.FIXED) {
            "固定目标"
        } else {
            "${state.stage.label} · 已连续达标 ${state.consecutiveMetWeeks} 周"
        }
    }

    private fun evaluateOneCompletedWeek(
        state: PlanState,
        weekStart: LocalDate,
        workouts: List<Workout>,
        targets: StageTargets,
        weeksToAdvance: Int,
    ): PlanState {
        val stats = WeekCalculator.statsForWeek(workouts, weekStart)
        val met = stats.isMet(targets.aerobic(state.stage), targets.strength(state.stage))
        val spent = state.weeksSpent.toMutableMap()
        spent[state.stage] = (spent[state.stage] ?: 0) + 1

        return if (met) {
            val streak = state.consecutiveMetWeeks + 1
            val next = state.stage.nextOrNull()
            if (streak >= weeksToAdvance && next != null) {
                state.copy(
                    stage = next,
                    consecutiveMetWeeks = 0,
                    lastEvaluatedWeekStart = weekStart,
                    weeksSpent = spent,
                    pendingAdvanceMessage = advanceMessage(next, targets),
                )
            } else {
                state.copy(
                    consecutiveMetWeeks = streak,
                    lastEvaluatedWeekStart = weekStart,
                    weeksSpent = spent,
                    pendingAdvanceMessage = null,
                )
            }
        } else {
            state.copy(
                consecutiveMetWeeks = 0,
                lastEvaluatedWeekStart = weekStart,
                weeksSpent = spent,
                pendingAdvanceMessage = null,
            )
        }
    }

    private fun advanceMessage(next: Stage, targets: StageTargets): String {
        return "已升到${next.label}（有氧 ${targets.aerobic(next)} · 力量 ${targets.strength(next)}）"
    }
}
