package com.personal.exercise.data

import android.content.Context
import androidx.room.withTransaction
import com.personal.exercise.domain.ActivityItem
import com.personal.exercise.domain.AppSettings
import com.personal.exercise.domain.Category
import com.personal.exercise.domain.Metric
import com.personal.exercise.domain.MetricTest
import com.personal.exercise.domain.PlanEngine
import com.personal.exercise.domain.Presets
import com.personal.exercise.domain.TargetMode
import com.personal.exercise.domain.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ExerciseRepository(private val db: AppDatabase) {

    val workouts: Flow<List<Workout>> = db.workouts().observeAll().map { list -> list.map { it.toDomain() } }
    val activities: Flow<List<ActivityItem>> = db.activities().observeAll().map { list -> list.map { it.toDomain() } }
    val metrics: Flow<List<Metric>> = db.metrics().observeAll().map { list -> list.map { it.toDomain() } }
    val tests: Flow<List<MetricTest>> = db.tests().observeAll().map { list -> list.map { it.toDomain() } }
    val settings: Flow<AppSettings> = db.settings().observe().map { it?.toDomain() ?: AppSettings() }

    suspend fun seedIfNeeded() {
        if (db.settings().get() == null) {
            db.settings().upsert(AppSettings().toEntity())
        }
        if (db.activities().count() == 0) {
            val seeded = Presets.activities.flatMap { (cat, names) ->
                names.map { name ->
                    ActivityEntity(
                        category = cat.name,
                        name = name,
                        isPreset = true,
                        hidden = false,
                    )
                }
            }
            db.activities().insertAll(seeded)
        }
        if (db.metrics().count() == 0) {
            db.metrics().insertAll(Presets.metrics.map { it.toEntity() })
        }
    }

    suspend fun settingsOnce(): AppSettings = db.settings().get()?.toDomain() ?: AppSettings()

    suspend fun workoutsOnce(): List<Workout> = db.workouts().getAll().map { it.toDomain() }

    suspend fun evaluatePlan(today: LocalDate = LocalDate.now()) {
        val settings = settingsOnce()
        val updated = PlanEngine.evaluate(
            state = settings.plan,
            today = today,
            mode = settings.targetMode,
            workouts = workoutsOnce(),
            targets = settings.stageTargets,
            weeksToAdvance = settings.weeksToAdvance,
        )
        if (updated != settings.plan) {
            db.settings().upsert(settings.copy(plan = updated).toEntity())
        }
    }

    suspend fun clearAdvanceMessage() {
        val settings = settingsOnce()
        if (settings.plan.pendingAdvanceMessage != null) {
            db.settings().upsert(
                settings.copy(plan = settings.plan.copy(pendingAdvanceMessage = null)).toEntity(),
            )
        }
    }

    suspend fun saveWorkout(workout: Workout) {
        if (workout.id == 0L) {
            db.workouts().insert(workout.toEntity())
        } else {
            val existing = db.workouts().get(workout.id)
            db.workouts().update(workout.toEntity(createdAt = existing?.createdAt ?: System.currentTimeMillis()))
        }
        evaluatePlan()
    }

    suspend fun deleteWorkout(id: Long) {
        db.workouts().delete(id)
        evaluatePlan()
    }

    suspend fun getWorkout(id: Long): Workout? = db.workouts().get(id)?.toDomain()

    suspend fun addCustomActivity(category: Category, name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val existing = db.activities().getAll().firstOrNull { it.category == category.name && it.name == trimmed }
        if (existing != null) {
            if (existing.hidden && !existing.isPreset) {
                db.activities().update(existing.copy(hidden = false))
            }
            return existing.id
        }
        return db.activities().insert(
            ActivityEntity(category = category.name, name = trimmed, isPreset = false, hidden = false),
        )
    }

    suspend fun renameActivity(id: Long, name: String) {
        val current = db.activities().getAll().first { it.id == id }
        check(!current.isPreset)
        db.activities().update(current.copy(name = name.trim()))
    }

    suspend fun setActivityHidden(id: Long, hidden: Boolean) {
        val current = db.activities().getAll().first { it.id == id }
        check(!current.isPreset)
        db.activities().update(current.copy(hidden = hidden))
    }

    suspend fun addMetric(name: String, unit: String, higherIsBetter: Boolean): Long {
        return db.metrics().insert(
            MetricEntity(name = name.trim(), unit = unit.trim(), higherIsBetter = higherIsBetter),
        )
    }

    suspend fun updateMetric(metric: Metric) {
        db.metrics().update(metric.toEntity())
    }

    suspend fun deleteMetric(id: Long) {
        db.tests().deleteForMetric(id)
        db.metrics().delete(id)
    }

    suspend fun addTest(metricId: Long, date: LocalDate, value: Double): Long {
        return db.tests().insert(
            MetricTestEntity(metricId = metricId, dateEpochDay = date.toEpochDay(), value = value),
        )
    }

    suspend fun updateTest(test: MetricTest) {
        db.tests().update(test.toEntity())
    }

    suspend fun deleteTest(id: Long) {
        db.tests().delete(id)
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = settingsOnce()
        db.settings().upsert(transform(current).toEntity())
    }

    suspend fun setTargetMode(mode: TargetMode) {
        updateSettings { current ->
            val plan = if (mode == TargetMode.PLAN && current.targetMode == TargetMode.FIXED) {
                PlanEngine.freezeEvaluationAt(LocalDate.now(), current.plan)
            } else current.plan
            current.copy(targetMode = mode, plan = plan)
        }
        evaluatePlan()
    }

    suspend fun resetPlan() {
        updateSettings { it.copy(plan = PlanEngine.resetKeepingHistoryUnevaluated(LocalDate.now())) }
    }

    suspend fun exportJson(): String {
        return BackupCodec.toJson(
            settings = settingsOnce(),
            workouts = workoutsOnce(),
            activities = db.activities().getAll().map { it.toDomain() },
            metrics = db.metrics().getAll().map { it.toDomain() },
            tests = db.tests().getAll().map { it.toDomain() },
        )
    }

    suspend fun exportCsv(): String = BackupCodec.workoutsCsv(workoutsOnce())

    suspend fun restoreJson(text: String) {
        val payload = BackupCodec.fromJson(text)
        db.withTransaction {
            db.workouts().deleteAll()
            db.activities().deleteAll()
            db.tests().deleteAll()
            db.metrics().deleteAll()
            db.settings().upsert(payload.settings)
            if (payload.workouts.isNotEmpty()) db.workouts().insertAll(payload.workouts)
            if (payload.activities.isNotEmpty()) db.activities().insertAll(payload.activities)
            val idMap = mutableMapOf<Long, Long>()
            payload.metricsWithOldId.forEach { (oldId, entity) ->
                val newId = db.metrics().insert(entity)
                idMap[oldId] = newId
            }
            val remapped = payload.testsByOldMetricId.mapNotNull { (oldMetricId, test) ->
                val newId = idMap[oldMetricId] ?: return@mapNotNull null
                test.copy(metricId = newId)
            }
            if (remapped.isNotEmpty()) db.tests().insertAll(remapped)
        }
        seedIfNeeded()
    }

    companion object {
        fun create(context: Context): ExerciseRepository =
            ExerciseRepository(AppDatabase.create(context.applicationContext))
    }
}
