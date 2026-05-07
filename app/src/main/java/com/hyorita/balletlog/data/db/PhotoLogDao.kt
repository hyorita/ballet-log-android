package com.hyorita.balletlog.data.db

import androidx.room.*
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.data.model.PhotoLogTag
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoLogDao {
    @Query("SELECT * FROM photo_logs ORDER BY date DESC")
    fun getAll(): Flow<List<PhotoLog>>

    @Query("SELECT * FROM photo_logs WHERE id = :id")
    suspend fun getById(id: String): PhotoLog?

    @Query("SELECT * FROM photo_logs WHERE isFavorite = 1 ORDER BY date DESC")
    fun getFavorites(): Flow<List<PhotoLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: PhotoLog)

    @Update
    suspend fun update(log: PhotoLog)

    @Delete
    suspend fun delete(log: PhotoLog)

    // --- Tag autocomplete pool ---

    @Query("SELECT * FROM photo_log_tags WHERE tagType = :type ORDER BY usageCount DESC, value ASC")
    fun getTagsByType(type: String): Flow<List<PhotoLogTag>>

    @Query("SELECT * FROM photo_log_tags WHERE tagType = :type AND value = :value LIMIT 1")
    suspend fun findTag(type: String, value: String): PhotoLogTag?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: PhotoLogTag)

    @Query("UPDATE photo_log_tags SET usageCount = usageCount + 1 WHERE tagType = :type AND value = :value")
    suspend fun bumpTag(type: String, value: String)

    @Delete
    suspend fun deleteTag(tag: PhotoLogTag)

    @Transaction
    suspend fun upsertTag(type: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        val existing = findTag(type, trimmed)
        if (existing == null) {
            insertTag(PhotoLogTag(tagType = type, value = trimmed))
        } else {
            bumpTag(type, trimmed)
        }
    }
}
