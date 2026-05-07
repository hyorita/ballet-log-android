package com.hyorita.balletlog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

@Entity(tableName = "photo_logs")
data class PhotoLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val photoPath: String = "",
    val filteredPhotoPath: String? = null,
    val filterName: String = "Original",
    val caption: String = "",
    val captionX: Double = 0.0,
    val captionY: Double = 0.0,
    val captionScale: Double = 1.0,
    val captionIsWhite: Boolean = true,
    val kcal: Int? = null,
    val durationMin: Int? = null,
    val avgBPM: Int? = null,
    val maxBPM: Int? = null,
    val tagsJson: String = "[]",
    val isFavorite: Boolean = false,
    val date: Long = System.currentTimeMillis()
) {
    val tags: List<String>
        get() = Gson().fromJson(tagsJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    companion object {
        fun create(
            photoPath: String,
            filteredPhotoPath: String? = null,
            filterName: String = "Original",
            caption: String = "",
            captionX: Double = 0.0,
            captionY: Double = 0.0,
            captionScale: Double = 1.0,
            captionIsWhite: Boolean = true,
            kcal: Int? = null,
            durationMin: Int? = null,
            avgBPM: Int? = null,
            maxBPM: Int? = null,
            tags: List<String> = emptyList(),
            isFavorite: Boolean = false,
            date: Long = System.currentTimeMillis()
        ) = PhotoLog(
            photoPath = photoPath,
            filteredPhotoPath = filteredPhotoPath,
            filterName = filterName,
            caption = caption,
            captionX = captionX,
            captionY = captionY,
            captionScale = captionScale,
            captionIsWhite = captionIsWhite,
            kcal = kcal,
            durationMin = durationMin,
            avgBPM = avgBPM,
            maxBPM = maxBPM,
            tagsJson = Gson().toJson(tags),
            isFavorite = isFavorite,
            date = date
        )
    }
}
