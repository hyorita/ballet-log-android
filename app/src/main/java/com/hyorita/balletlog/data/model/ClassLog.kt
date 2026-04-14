package com.hyorita.balletlog.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

@Entity(tableName = "class_logs")
data class ClassLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val barreStepsJson: String = "[]",
    val centerStepsJson: String = "[]",
    val photosJson: String = "[]",
    val barreMusic: String = "",
    val centerMusic: String = "",
    val notes: String = "",
    val workoutJson: String? = null,
    val favorite: Boolean = false,
    @ColumnInfo(name = "view_count", defaultValue = "0") val viewCount: Int = 0
) {
    private val gson get() = Gson()

    var barreSteps: List<Step>
        get() = gson.fromJson(barreStepsJson, object : TypeToken<List<Step>>() {}.type) ?: emptyList()
        set(_) {}

    var centerSteps: List<Step>
        get() = gson.fromJson(centerStepsJson, object : TypeToken<List<Step>>() {}.type) ?: emptyList()
        set(_) {}

    var photos: List<PhotoItem>
        get() = gson.fromJson(photosJson, object : TypeToken<List<PhotoItem>>() {}.type) ?: emptyList()
        set(_) {}

    var workout: WorkoutInfo?
        get() = workoutJson?.let { gson.fromJson(it, WorkoutInfo::class.java) }
        set(_) {}

    companion object {
        fun create(
            date: Long = System.currentTimeMillis(),
            barreSteps: List<Step> = emptyList(),
            centerSteps: List<Step> = emptyList(),
            photos: List<PhotoItem> = emptyList(),
            barreMusic: String = "",
            centerMusic: String = "",
            notes: String = "",
            workout: WorkoutInfo? = null,
            favorite: Boolean = false,
            viewCount: Int = 0
        ): ClassLog {
            val gson = Gson()
            return ClassLog(
                date = date,
                barreStepsJson = gson.toJson(barreSteps),
                centerStepsJson = gson.toJson(centerSteps),
                photosJson = gson.toJson(photos),
                barreMusic = barreMusic,
                centerMusic = centerMusic,
                notes = notes,
                workoutJson = workout?.let { gson.toJson(it) },
                favorite = favorite,
                viewCount = viewCount
            )
        }
    }
}
