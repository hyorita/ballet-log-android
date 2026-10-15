package com.hyorita.balletlog.data

import android.content.Context
import com.hyorita.balletlog.data.db.BalletLogDatabase

/**
 * "Is this an existing user?" for the what's-new badges (1.16 Stats tab,
 * 1.16.1 Settings dot). Records are the one signal that's true for a real
 * existing install by any path — a tab-specific flag misses people who never
 * touched that tab and doesn't survive a backup restore. Same rule as iOS.
 */
suspend fun hasAnyRecords(context: Context): Boolean {
    val db = BalletLogDatabase.getInstance(context)
    return db.classLogDao().count() > 0 ||
        db.photoLogDao().count() > 0 ||
        db.noteDao().count() > 0
}
