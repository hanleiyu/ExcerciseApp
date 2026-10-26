package com.personal.exercise.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAll(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun get(id: Long): WorkoutEntity?

    @Insert
    suspend fun insert(entity: WorkoutEntity): Long

    @Update
    suspend fun update(entity: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workouts")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<WorkoutEntity>)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY isPreset DESC, id ASC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities")
    suspend fun getAll(): List<ActivityEntity>

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ActivityEntity>)

    @Update
    suspend fun update(entity: ActivityEntity)

    @Query("DELETE FROM activities")
    suspend fun deleteAll()
}

@Dao
interface MetricDao {
    @Query("SELECT * FROM metrics ORDER BY id ASC")
    fun observeAll(): Flow<List<MetricEntity>>

    @Query("SELECT * FROM metrics")
    suspend fun getAll(): List<MetricEntity>

    @Query("SELECT * FROM metrics WHERE id = :id")
    suspend fun get(id: Long): MetricEntity?

    @Query("SELECT COUNT(*) FROM metrics")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: MetricEntity): Long

    @Insert
    suspend fun insertAll(items: List<MetricEntity>)

    @Update
    suspend fun update(entity: MetricEntity)

    @Query("DELETE FROM metrics WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM metrics")
    suspend fun deleteAll()
}

@Dao
interface MetricTestDao {
    @Query("SELECT * FROM metric_tests ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<MetricTestEntity>>

    @Query("SELECT * FROM metric_tests WHERE metricId = :metricId ORDER BY dateEpochDay ASC, id ASC")
    fun observeForMetric(metricId: Long): Flow<List<MetricTestEntity>>

    @Query("SELECT * FROM metric_tests")
    suspend fun getAll(): List<MetricTestEntity>

    @Insert
    suspend fun insert(entity: MetricTestEntity): Long

    @Update
    suspend fun update(entity: MetricTestEntity)

    @Query("DELETE FROM metric_tests WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM metric_tests WHERE metricId = :metricId")
    suspend fun deleteForMetric(metricId: Long)

    @Query("DELETE FROM metric_tests")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<MetricTestEntity>)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SettingsEntity)
}
