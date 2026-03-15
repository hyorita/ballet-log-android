package com.hyorita.balletlog.data.model

data class WorkoutInfo(
    val durationMinutes: Int = 0,
    val activeCalories: Int = 0,
    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val sourceName: String = ""
)
