package com.hyorita.balletlog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val tagsJson: String = "[]",
    val photosJson: String = "[]",   // List<String> 파일명
    val linkedLogId: String? = null,
    val urlLink: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val tags: List<String>
        get() = Gson().fromJson(tagsJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    val photoFileNames: List<String>
        get() = Gson().fromJson(photosJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    companion object {
        fun create(
            title: String = "",
            content: String = "",
            tags: List<String> = emptyList(),
            photoFileNames: List<String> = emptyList(),
            linkedLogId: String? = null,
            urlLink: String? = null,
            pinned: Boolean = false
        ) = Note(
            title = title,
            content = content,
            tagsJson = Gson().toJson(tags),
            photosJson = Gson().toJson(photoFileNames),
            linkedLogId = linkedLogId,
            urlLink = urlLink,
            pinned = pinned
        )
    }
}
