package com.hyorita.balletlog.data.model

data class WorkoutInfo(
    val durationMinutes: Int = 0,
    val activeCalories: Int = 0,
    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val sourceName: String = "",
    // 1.9: Health Connect ExerciseSessionRecord.metadata.id of the session
    // this workout came from. Lets Stats dedupe a ClassLog workout against the
    // same session imported as a PhotoLog placeholder. Lives inside the Gson
    // workoutJson blob, so adding it needs no Room migration and stays
    // backup-compatible with 1.8. Null for hand-entered workouts.
    val externalWorkoutId: String? = null
)
