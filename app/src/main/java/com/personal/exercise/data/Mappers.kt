package com.personal.exercise.data

import com.personal.exercise.domain.ActivityItem
import com.personal.exercise.domain.AppSettings
import com.personal.exercise.domain.Category
import com.personal.exercise.domain.Intensity
import com.personal.exercise.domain.Metric
import com.personal.exercise.domain.MetricTest
import com.personal.exercise.domain.PlanState
import com.personal.exercise.domain.Stage
import com.personal.exercise.domain.StageTargets
import com.personal.exercise.domain.TargetMode
import com.personal.exercise.domain.Workout
import org.json.JSONObject
import java.time.LocalDate

fun WorkoutEntity.toDomain() = Workout(
    id = id,
    category = Category.valueOf(category),
    activityName = activityName,
    durationMinutes = durationMinutes,
    intensity = intensity?.let { Intensity.valueOf(it) },
    date = LocalDate.ofEpochDay(dateEpochDay),
    note = note,
    isFree = isFree,
)

fun Workout.toEntity(createdAt: Long = System.currentTimeMillis()) = WorkoutEntity(
    id = id,
    category = category.name,
    activityName = activityName,
    durationMinutes = durationMinutes,
    intensity = intensity?.name,
    dateEpochDay = date.toEpochDay(),
    note = note,
    createdAt = createdAt,
    isFree = isFree,
)

fun ActivityEntity.toDomain() = ActivityItem(
    id = id,
    category = Category.valueOf(category),
    name = name,
    isPreset = isPreset,
    hidden = hidden,
)

fun ActivityItem.toEntity() = ActivityEntity(
    id = id,
    category = category.name,
    name = name,
    isPreset = isPreset,
    hidden = hidden,
)

fun MetricEntity.toDomain() = Metric(
    id = id,
    name = name,
    unit = unit,
    higherIsBetter = higherIsBetter,
)

fun Metric.toEntity() = MetricEntity(
    id = id,
    name = name,
    unit = unit,
    higherIsBetter = higherIsBetter,
)

fun MetricTestEntity.toDomain() = MetricTest(
    id = id,
    metricId = metricId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    value = value,
)

fun MetricTest.toEntity() = MetricTestEntity(
    id = id,
    metricId = metricId,
    dateEpochDay = date.toEpochDay(),
    value = value,
)

fun SettingsEntity.toDomain(): AppSettings = AppSettings(
    targetMode = TargetMode.valueOf(targetMode),
    fixedAerobic = fixedAerobic,
    fixedStrength = fixedStrength,
    balanceGoal = balanceGoal,
    stageTargets = StageTargets(
        stage1Aerobic = stage1Aerobic,
        stage1Strength = stage1Strength,
        stage2Aerobic = stage2Aerobic,
        stage2Strength = stage2Strength,
        stage3Aerobic = stage3Aerobic,
        stage3Strength = stage3Strength,
    ),
    weeksToAdvance = weeksToAdvance.coerceAtLeast(1),
    plan = PlanState(
        stage = Stage.valueOf(stage),
        consecutiveMetWeeks = consecutiveMetWeeks,
        lastEvaluatedWeekStart = lastEvaluatedEpochDay.takeIf { it >= 0 }?.let { LocalDate.ofEpochDay(it) },
        weeksSpent = parseWeeksSpent(weeksSpentJson),
        pendingAdvanceMessage = pendingAdvanceMessage,
    ),
)

fun AppSettings.toEntity(): SettingsEntity = SettingsEntity(
    targetMode = targetMode.name,
    fixedAerobic = fixedAerobic,
    fixedStrength = fixedStrength,
    balanceGoal = balanceGoal,
    stage = plan.stage.name,
    consecutiveMetWeeks = plan.consecutiveMetWeeks,
    lastEvaluatedEpochDay = plan.lastEvaluatedWeekStart?.toEpochDay() ?: -1L,
    weeksSpentJson = encodeWeeksSpent(plan.weeksSpent),
    pendingAdvanceMessage = plan.pendingAdvanceMessage,
    stage1Aerobic = stageTargets.stage1Aerobic,
    stage1Strength = stageTargets.stage1Strength,
    stage2Aerobic = stageTargets.stage2Aerobic,
    stage2Strength = stageTargets.stage2Strength,
    stage3Aerobic = stageTargets.stage3Aerobic,
    stage3Strength = stageTargets.stage3Strength,
    weeksToAdvance = weeksToAdvance.coerceAtLeast(1),
)

fun parseWeeksSpent(json: String): Map<Stage, Int> {
    val obj = JSONObject(json)
    return Stage.entries.associateWith { obj.optInt(it.name, 0) }
}

fun encodeWeeksSpent(map: Map<Stage, Int>): String {
    val obj = JSONObject()
    Stage.entries.forEach { obj.put(it.name, map[it] ?: 0) }
    return obj.toString()
}
