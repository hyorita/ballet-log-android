package com.hyorita.balletlog.data.db

import androidx.room.*
import com.hyorita.balletlog.data.model.ClassLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassLogDao {
    @Query("SELECT * FROM class_logs ORDER BY date DESC")
    fun getAll(): Flow<List<ClassLog>>

    @Query("SELECT * FROM class_logs WHERE id = :id")
    suspend fun getById(id: String): ClassLog?

    @Query("SELECT * FROM class_logs WHERE favorite = 1 ORDER BY date DESC")
    fun getFavorites(): Flow<List<ClassLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ClassLog)

    @Update
    suspend fun update(log: ClassLog)

    @Delete
    suspend fun delete(log: ClassLog)
}
