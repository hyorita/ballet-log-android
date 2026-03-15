package com.hyorita.balletlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note

@Database(entities = [ClassLog::class, Note::class], version = 3, exportSchema = false)
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
                    .fallbackToDestructiveMigration() // 개발 중이라 파괴적 마이그레이션 허용
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
