package com.personal.exercise.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutEntity::class,
        ActivityEntity::class,
        MetricEntity::class,
        MetricTestEntity::class,
        SettingsEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workouts(): WorkoutDao
    abstract fun activities(): ActivityDao
    abstract fun metrics(): MetricDao
    abstract fun tests(): MetricTestDao
    abstract fun settings(): SettingsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN stage1Aerobic INTEGER NOT NULL DEFAULT 60")
                db.execSQL("ALTER TABLE settings ADD COLUMN stage1Strength INTEGER NOT NULL DEFAULT 40")
                db.execSQL("ALTER TABLE settings ADD COLUMN stage2Aerobic INTEGER NOT NULL DEFAULT 80")
                db.execSQL("ALTER TABLE settings ADD COLUMN stage2Strength INTEGER NOT NULL DEFAULT 50")
                db.execSQL("ALTER TABLE settings ADD COLUMN stage3Aerobic INTEGER NOT NULL DEFAULT 100")
                db.execSQL("ALTER TABLE settings ADD COLUMN stage3Strength INTEGER NOT NULL DEFAULT 60")
                db.execSQL("ALTER TABLE settings ADD COLUMN weeksToAdvance INTEGER NOT NULL DEFAULT 2")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "exercise.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
