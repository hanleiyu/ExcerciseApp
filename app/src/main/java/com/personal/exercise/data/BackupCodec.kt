package com.personal.exercise.data

import com.personal.exercise.domain.AppSettings
import com.personal.exercise.domain.toIso
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object BackupCodec {
    private const val VERSION = 1

    fun toJson(
        settings: AppSettings,
        workouts: List<com.personal.exercise.domain.Workout>,
        activities: List<com.personal.exercise.domain.ActivityItem>,
        metrics: List<com.personal.exercise.domain.Metric>,
        tests: List<com.personal.exercise.domain.MetricTest>,
    ): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", LocalDate.now().toIso())
        root.put("settings", JSONObject().apply {
            put("targetMode", settings.targetMode.name)
            put("fixedAerobic", settings.fixedAerobic)
            put("fixedStrength", settings.fixedStrength)
            put("balanceGoal", settings.balanceGoal)
            put("stage", settings.plan.stage.name)
            put("consecutiveMetWeeks", settings.plan.consecutiveMetWeeks)
            put("lastEvaluatedEpochDay", settings.plan.lastEvaluatedWeekStart?.toEpochDay() ?: -1L)
            put("weeksSpentJson", encodeWeeksSpent(settings.plan.weeksSpent))
            put("pendingAdvanceMessage", settings.plan.pendingAdvanceMessage)
            put("stage1Aerobic", settings.stageTargets.stage1Aerobic)
            put("stage1Strength", settings.stageTargets.stage1Strength)
            put("stage2Aerobic", settings.stageTargets.stage2Aerobic)
            put("stage2Strength", settings.stageTargets.stage2Strength)
            put("stage3Aerobic", settings.stageTargets.stage3Aerobic)
            put("stage3Strength", settings.stageTargets.stage3Strength)
            put("weeksToAdvance", settings.weeksToAdvance)
        })
        root.put("workouts", JSONArray().apply {
            workouts.forEach { w ->
                put(JSONObject().apply {
                    put("category", w.category.name)
                    put("activityName", w.activityName)
                    put("durationMinutes", w.durationMinutes)
                    put("intensity", w.intensity?.name)
                    put("date", w.date.toIso())
                    put("note", w.note)
                })
            }
        })
        root.put("activities", JSONArray().apply {
            activities.forEach { a ->
                put(JSONObject().apply {
                    put("category", a.category.name)
                    put("name", a.name)
                    put("isPreset", a.isPreset)
                    put("hidden", a.hidden)
                })
            }
        })
        root.put("metrics", JSONArray().apply {
            metrics.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("unit", m.unit)
                    put("higherIsBetter", m.higherIsBetter)
                })
            }
        })
        root.put("tests", JSONArray().apply {
            tests.forEach { t ->
                put(JSONObject().apply {
                    put("metricId", t.metricId)
                    put("date", t.date.toIso())
                    put("value", t.value)
                })
            }
        })
        return root.toString(2)
    }

    fun fromJson(text: String): BackupPayload {
        val root = JSONObject(text)
        val s = root.getJSONObject("settings")
        val settings = SettingsEntity(
            targetMode = s.getString("targetMode"),
            fixedAerobic = s.getInt("fixedAerobic"),
            fixedStrength = s.getInt("fixedStrength"),
            balanceGoal = s.getInt("balanceGoal"),
            stage = s.getString("stage"),
            consecutiveMetWeeks = s.getInt("consecutiveMetWeeks"),
            lastEvaluatedEpochDay = s.optLong("lastEvaluatedEpochDay", -1L),
            weeksSpentJson = s.optString(
                "weeksSpentJson",
                """{"STAGE_1":0,"STAGE_2":0,"STAGE_3":0,"MAINTAIN":0}""",
            ),
            pendingAdvanceMessage = s.optString("pendingAdvanceMessage").ifBlank { null },
            stage1Aerobic = s.optInt("stage1Aerobic", 60),
            stage1Strength = s.optInt("stage1Strength", 40),
            stage2Aerobic = s.optInt("stage2Aerobic", 80),
            stage2Strength = s.optInt("stage2Strength", 50),
            stage3Aerobic = s.optInt("stage3Aerobic", 100),
            stage3Strength = s.optInt("stage3Strength", 60),
            weeksToAdvance = s.optInt("weeksToAdvance", 2).coerceAtLeast(1),
        )
        val workouts = root.getJSONArray("workouts").mapObjects { o ->
            WorkoutEntity(
                category = o.getString("category"),
                activityName = o.getString("activityName"),
                durationMinutes = o.getInt("durationMinutes"),
                intensity = o.optString("intensity").ifBlank { null },
                dateEpochDay = LocalDate.parse(o.getString("date")).toEpochDay(),
                note = o.optString("note").ifBlank { null },
                createdAt = System.currentTimeMillis(),
            )
        }
        val activities = root.getJSONArray("activities").mapObjects { o ->
            ActivityEntity(
                category = o.getString("category"),
                name = o.getString("name"),
                isPreset = o.getBoolean("isPreset"),
                hidden = o.getBoolean("hidden"),
            )
        }
        val oldMetrics = root.getJSONArray("metrics").mapObjects { o ->
            o.optLong("id") to MetricEntity(
                name = o.getString("name"),
                unit = o.getString("unit"),
                higherIsBetter = o.getBoolean("higherIsBetter"),
            )
        }
        val testsByOldMetric = root.optJSONArray("tests")?.mapObjects { o ->
            o.getLong("metricId") to MetricTestEntity(
                metricId = 0,
                dateEpochDay = LocalDate.parse(o.getString("date")).toEpochDay(),
                value = o.getDouble("value"),
            )
        } ?: emptyList()
        return BackupPayload(settings, workouts, activities, oldMetrics, testsByOldMetric)
    }

    fun workoutsCsv(workouts: List<com.personal.exercise.domain.Workout>): String {
        val header = "日期,类别,项目,时长分钟,强度,计入分钟,备注"
        val rows = workouts.sortedBy { it.date }.map { w ->
            listOf(
                w.date.toIso(),
                w.category.label,
                csv(w.activityName),
                w.durationMinutes.toString(),
                w.intensity?.label.orEmpty(),
                w.creditedMinutes.toString(),
                csv(w.note.orEmpty()),
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun csv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
    }

    private fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> {
        return (0 until length()).map { block(getJSONObject(it)) }
    }
}

data class BackupPayload(
    val settings: SettingsEntity,
    val workouts: List<WorkoutEntity>,
    val activities: List<ActivityEntity>,
    val metricsWithOldId: List<Pair<Long, MetricEntity>>,
    val testsByOldMetricId: List<Pair<Long, MetricTestEntity>>,
)
