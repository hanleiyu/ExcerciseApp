package com.personal.exercise.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val activityName: String,
    val durationMinutes: Int,
    val intensity: String?,
    val dateEpochDay: Long,
    val note: String?,
    val createdAt: Long,
    val isFree: Boolean = false,
)

@Entity(
    tableName = "activities",
    indices = [Index(value = ["category", "name"], unique = true)],
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val name: String,
    val isPreset: Boolean,
    val hidden: Boolean,
)

@Entity(tableName = "metrics")
data class MetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val higherIsBetter: Boolean,
)

@Entity(
    tableName = "metric_tests",
    indices = [Index("metricId")],
)
data class MetricTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metricId: Long,
    val dateEpochDay: Long,
    val value: Double,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val targetMode: String = "PLAN",
    val fixedAerobic: Int = 100,
    val fixedStrength: Int = 60,
    val balanceGoal: Int = 2,
    val stage: String = "STAGE_1",
    val consecutiveMetWeeks: Int = 0,
    val lastEvaluatedEpochDay: Long = -1,
    val weeksSpentJson: String = """{"STAGE_1":0,"STAGE_2":0,"STAGE_3":0,"MAINTAIN":0}""",
    val pendingAdvanceMessage: String? = null,
    val stage1Aerobic: Int = 60,
    val stage1Strength: Int = 40,
    val stage2Aerobic: Int = 80,
    val stage2Strength: Int = 50,
    val stage3Aerobic: Int = 100,
    val stage3Strength: Int = 60,
    val weeksToAdvance: Int = 2,
)
