package com.hyorita.balletlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.data.model.PhotoLogTag

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE class_logs ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1.8: Health Connect auto-import dedupe key.
        database.execSQL(
            "ALTER TABLE photo_logs ADD COLUMN externalWorkoutId TEXT"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_photo_logs_externalWorkoutId ON photo_logs(externalWorkoutId)"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS photo_logs (
                id TEXT NOT NULL PRIMARY KEY,
                photoPath TEXT NOT NULL,
                filteredPhotoPath TEXT,
                filterName TEXT NOT NULL,
                caption TEXT NOT NULL,
                captionX REAL NOT NULL,
                captionY REAL NOT NULL,
                captionScale REAL NOT NULL,
                captionIsWhite INTEGER NOT NULL,
                kcal INTEGER,
                durationMin INTEGER,
                avgBPM INTEGER,
                maxBPM INTEGER,
                tagsJson TEXT NOT NULL,
                isFavorite INTEGER NOT NULL,
                date INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS photo_log_tags (
                id TEXT NOT NULL PRIMARY KEY,
                tagType TEXT NOT NULL,
                value TEXT NOT NULL,
                usageCount INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_photo_log_tags_tagType_value ON photo_log_tags(tagType, value)"
        )
    }
}

@Database(
    entities = [ClassLog::class, Note::class, PhotoLog::class, PhotoLogTag::class],
    version = 6,
    exportSchema = false
)
abstract class BalletLogDatabase : RoomDatabase() {
    abstract fun classLogDao(): ClassLogDao
    abstract fun noteDao(): NoteDao
    abstract fun photoLogDao(): PhotoLogDao

    companion object {
        @Volatile private var INSTANCE: BalletLogDatabase? = null

        fun getInstance(context: Context): BalletLogDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BalletLogDatabase::class.java,
                    "ballet_log_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { INSTANCE = it }
            }

        /**
         * Close the open Room handle so the on-disk files can be replaced
         * (used by BackupManager during import). Next `getInstance` call
         * re-opens against whatever's now on disk.
         */
        fun closeAndReset() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
