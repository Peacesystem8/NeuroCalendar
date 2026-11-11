package com.example.neurocalendar.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.neurocalendar.data.dao.CalendarDao
import com.example.neurocalendar.data.model.Event
import com.example.neurocalendar.data.model.Task
import com.example.neurocalendar.data.model.TaskOccurrenceCompletion

@Database(
    entities = [Event::class, Task::class, TaskOccurrenceCompletion::class],
    version = 4,
    exportSchema = false
)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN reminderMinutesBefore INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN repeatInterval INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN repeatUnit TEXT NOT NULL DEFAULT 'WEEK'"
                )
                db.execSQL("ALTER TABLE events ADD COLUMN repeatUntilMillis INTEGER")
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN repeatInterval INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN repeatUnit TEXT NOT NULL DEFAULT 'WEEK'"
                )
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatUntilMillis INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS task_occurrence_completions (" +
                        "taskId INTEGER NOT NULL, " +
                        "dueDateMillis INTEGER NOT NULL, " +
                        "isDismissed INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(taskId, dueDateMillis))"
                )
            }
        }

        @Volatile
        private var instance: CalendarDatabase? = null

        fun getInstance(context: Context): CalendarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CalendarDatabase::class.java,
                    "neurocalendar.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}
