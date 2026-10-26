package com.personal.exercise.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanEngineTest {

    private val week0 = LocalDate.of(2026, 9, 28) // Monday
    private val week1 = LocalDate.of(2026, 10, 5)
    private val week2 = LocalDate.of(2026, 10, 12)
    private val todayInWeek2 = LocalDate.of(2026, 10, 14) // Wednesday of week2

    @Test
    fun twoConsecutiveMetWeeksAdvanceStage() {
        val workouts = metWeek(week0) + metWeek(week1)
        val result = PlanEngine.evaluate(PlanState(), todayInWeek2, TargetMode.PLAN, workouts)
        assertEquals(Stage.STAGE_2, result.stage)
        assertEquals(0, result.consecutiveMetWeeks)
        assertEquals(2, result.weeksSpent[Stage.STAGE_1])
        assertEquals(0, result.weeksSpent[Stage.STAGE_2])
        assertTrue(result.pendingAdvanceMessage!!.contains("阶段 2"))
    }

    @Test
    fun missedWeekResetsStreakAndKeepsStage() {
        val workouts = metWeek(week0) + missWeek(week1)
        val afterFirst = PlanEngine.evaluate(PlanState(), week1.plusDays(2), TargetMode.PLAN, metWeek(week0))
        assertEquals(1, afterFirst.consecutiveMetWeeks)
        assertEquals(Stage.STAGE_1, afterFirst.stage)

        val afterMiss = PlanEngine.evaluate(afterFirst, todayInWeek2, TargetMode.PLAN, workouts)
        assertEquals(Stage.STAGE_1, afterMiss.stage)
        assertEquals(0, afterMiss.consecutiveMetWeeks)
        assertEquals(2, afterMiss.weeksSpent[Stage.STAGE_1])
        assertNull(afterMiss.pendingAdvanceMessage)
    }

    @Test
    fun nonConsecutiveMetWeeksDoNotAdvance() {
        val workouts = metWeek(week0) + missWeek(week1) + metWeek(week2)
        val today = week2.plusWeeks(1).plusDays(1)
        val result = PlanEngine.evaluate(PlanState(), today, TargetMode.PLAN, workouts)
        assertEquals(Stage.STAGE_1, result.stage)
        assertEquals(1, result.consecutiveMetWeeks)
        assertEquals(3, result.weeksSpent[Stage.STAGE_1])
    }

    @Test
    fun currentWeekIsNotEvaluated() {
        val workouts = metWeek(week2)
        val result = PlanEngine.evaluate(PlanState(), todayInWeek2, TargetMode.PLAN, workouts)
        assertEquals(Stage.STAGE_1, result.stage)
        assertEquals(0, result.consecutiveMetWeeks)
        assertNull(result.lastEvaluatedWeekStart)
        assertEquals(0, result.weeksSpent[Stage.STAGE_1])
    }

    @Test
    fun emptyCompletedWeekCountsAsMiss() {
        val workouts = metWeek(week0)
        val today = week2.plusDays(2)
        val result = PlanEngine.evaluate(PlanState(), today, TargetMode.PLAN, workouts)
        assertEquals(0, result.consecutiveMetWeeks)
        assertEquals(2, result.weeksSpent[Stage.STAGE_1])
    }

    @Test
    fun stage3AdvancesToMaintain() {
        var state = PlanState(stage = Stage.STAGE_3)
        val workouts = metWeek(week0, Stage.STAGE_3) + metWeek(week1, Stage.STAGE_3)
        val result = PlanEngine.evaluate(state, todayInWeek2, TargetMode.PLAN, workouts)
        assertEquals(Stage.MAINTAIN, result.stage)
        assertTrue(result.pendingAdvanceMessage!!.contains("维持期"))
    }

    @Test
    fun maintainDoesNotAdvanceFurther() {
        val state = PlanState(stage = Stage.MAINTAIN)
        val workouts = metWeek(week0, Stage.MAINTAIN) + metWeek(week1, Stage.MAINTAIN)
        val result = PlanEngine.evaluate(state, todayInWeek2, TargetMode.PLAN, workouts)
        assertEquals(Stage.MAINTAIN, result.stage)
        assertEquals(2, result.consecutiveMetWeeks)
        assertNull(result.pendingAdvanceMessage)
    }

    @Test
    fun fixedModeNeverAdvances() {
        val workouts = metWeek(week0) + metWeek(week1)
        val result = PlanEngine.evaluate(PlanState(), todayInWeek2, TargetMode.FIXED, workouts)
        assertEquals(Stage.STAGE_1, result.stage)
        assertEquals(0, result.consecutiveMetWeeks)
        assertNull(result.lastEvaluatedWeekStart)
    }

    @Test
    fun alreadyEvaluatedWeeksAreNotReprocessed() {
        val state = PlanState(
            stage = Stage.STAGE_2,
            consecutiveMetWeeks = 0,
            lastEvaluatedWeekStart = week1,
            weeksSpent = mapOf(Stage.STAGE_1 to 2, Stage.STAGE_2 to 0, Stage.STAGE_3 to 0, Stage.MAINTAIN to 0),
        )
        val workouts = metWeek(week0) + metWeek(week1)
        val result = PlanEngine.evaluate(state, todayInWeek2, TargetMode.PLAN, workouts)
        assertEquals(Stage.STAGE_2, result.stage)
        assertEquals(0, result.weeksSpent[Stage.STAGE_2])
        assertEquals(week1, result.lastEvaluatedWeekStart)
    }

    @Test
    fun resetStartsAtStage1AndSkipsPastWeeks() {
        val reset = PlanEngine.resetKeepingHistoryUnevaluated(todayInWeek2)
        assertEquals(Stage.STAGE_1, reset.stage)
        assertEquals(0, reset.consecutiveMetWeeks)
        assertEquals(week1, reset.lastEvaluatedWeekStart)
        val workouts = metWeek(week0) + metWeek(week1)
        val result = PlanEngine.evaluate(reset, todayInWeek2, TargetMode.PLAN, workouts)
        assertEquals(Stage.STAGE_1, result.stage)
        assertEquals(0, result.weeksSpent.values.sum())
    }

    @Test
    fun upgradeHintWhenStreakIsOne() {
        val state = PlanState(stage = Stage.STAGE_2, consecutiveMetWeeks = 1)
        val hint = PlanEngine.upgradeHint(state, TargetMode.PLAN)
        assertTrue(hint!!.contains("阶段 3"))
        assertNull(PlanEngine.upgradeHint(state.copy(consecutiveMetWeeks = 0), TargetMode.PLAN))
        assertNull(PlanEngine.upgradeHint(PlanState(stage = Stage.MAINTAIN, consecutiveMetWeeks = 1), TargetMode.PLAN))
        assertNull(PlanEngine.upgradeHint(state, TargetMode.FIXED))
    }

    @Test
    fun customWeeksToAdvanceNeedsThreeMets() {
        val workouts = metWeek(week0) + metWeek(week1)
        val result = PlanEngine.evaluate(
            PlanState(), todayInWeek2, TargetMode.PLAN, workouts, weeksToAdvance = 3,
        )
        assertEquals(Stage.STAGE_1, result.stage)
        assertEquals(2, result.consecutiveMetWeeks)
        assertNull(result.pendingAdvanceMessage)
    }

    @Test
    fun customStageTargetsUsedForMet() {
        val targets = StageTargets(stage1Aerobic = 30, stage1Strength = 20)
        val workouts = metWeek(week0, targets = targets) + metWeek(week1, targets = targets)
        val result = PlanEngine.evaluate(PlanState(), todayInWeek2, TargetMode.PLAN, workouts, targets)
        assertEquals(Stage.STAGE_2, result.stage)
        assertTrue(result.pendingAdvanceMessage!!.contains("阶段 2"))
    }

    @Test
    fun defaultWorkoutsMissCustomHighTargets() {
        val targets = StageTargets(stage1Aerobic = 200, stage1Strength = 200)
        val workouts = metWeek(week0) + metWeek(week1)
        val result = PlanEngine.evaluate(PlanState(), todayInWeek2, TargetMode.PLAN, workouts, targets)
        assertEquals(Stage.STAGE_1, result.stage)
        assertEquals(0, result.consecutiveMetWeeks)
    }

    @Test
    fun upgradeHintUsesConfiguredNextTargetsAndWeeks() {
        val state = PlanState(stage = Stage.STAGE_2, consecutiveMetWeeks = 1)
        val targets = StageTargets(stage3Aerobic = 90, stage3Strength = 55)
        val hint = PlanEngine.upgradeHint(state, TargetMode.PLAN, targets)
        assertTrue(hint!!.contains("90"))
        assertTrue(hint.contains("55"))
        assertNull(PlanEngine.upgradeHint(state, TargetMode.PLAN, weeksToAdvance = 3))
    }

    @Test
    fun stageLineIsNotCalendarBased() {
        val line = PlanEngine.stageLine(PlanState(stage = Stage.STAGE_2, consecutiveMetWeeks = 1), TargetMode.PLAN)
        assertEquals("阶段 2 · 已连续达标 1 周", line)
        assertEquals("固定目标", PlanEngine.stageLine(PlanState(), TargetMode.FIXED))
    }

    @Test
    fun balanceSessionsDoNotHelpAdvancement() {
        val workouts = listOf(
            workout(Category.AEROBIC, 80, Intensity.MID, week0),
            workout(Category.BALANCE, 60, null, week0),
            workout(Category.AEROBIC, 80, Intensity.MID, week1),
            workout(Category.BALANCE, 60, null, week1),
        )
        val result = PlanEngine.evaluate(PlanState(), todayInWeek2, TargetMode.PLAN, workouts)
        assertEquals(Stage.STAGE_1, result.stage)
        assertEquals(0, result.consecutiveMetWeeks)
    }

    private fun metWeek(
        weekStart: LocalDate,
        stage: Stage = Stage.STAGE_1,
        targets: StageTargets = StageTargets(),
    ): List<Workout> {
        return listOf(
            workout(Category.AEROBIC, targets.aerobic(stage), Intensity.MID, weekStart),
            workout(Category.STRENGTH, targets.strength(stage), null, weekStart.plusDays(1)),
        )
    }

    private fun missWeek(weekStart: LocalDate): List<Workout> {
        return listOf(workout(Category.STRENGTH, 5, null, weekStart))
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
