package com.hyorita.balletlog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "photo_log_tags",
    indices = [Index(value = ["tagType", "value"], unique = true)]
)
data class PhotoLogTag(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tagType: String,        // "studio" | "level" | "teacher"
    val value: String,
    val usageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
