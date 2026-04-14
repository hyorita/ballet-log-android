package com.hyorita.balletlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE class_logs ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0"
        )
    }
}

@Database(entities = [ClassLog::class, Note::class], version = 4, exportSchema = false)
abstract class BalletLogDatabase : RoomDatabase() {
    abstract fun classLogDao(): ClassLogDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var INSTANCE: BalletLogDatabase? = null

        fun getInstance(context: Context): BalletLogDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BalletLogDatabase::class.java,
                    "ballet_log_db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
