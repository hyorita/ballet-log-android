package com.hyorita.balletlog.util

import android.util.Log
import com.hyorita.balletlog.BuildConfig

/**
 * 1.9: debug-only logging. Compiles out in release builds — the early return
 * on `!BuildConfig.DEBUG` lets R8 strip the call entirely, so shipping builds
 * stay quiet (mirrors iOS's debugLog() replacing print()).
 */
fun debugLog(tag: String, message: String, error: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (error != null) Log.d(tag, message, error) else Log.d(tag, message)
}
