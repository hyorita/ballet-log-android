package com.hyorita.balletlog.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyorita.balletlog.data.HealthConnectManager
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.PhotoLogTag
import com.hyorita.balletlog.data.model.WorkoutInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BalletLogDatabase.getInstance(app)
    private val dao = db.classLogDao()
    // 1.15: studio/level/teacher tags share the Log tab's pool — one
    // autocomplete list, not a parallel one per tab.
    private val photoLogDao = db.photoLogDao()

    val logs = dao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val studioTags = photoLogDao.getTagsByType("studio").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val levelTags = photoLogDao.getTagsByType("level").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val teacherTags = photoLogDao.getTagsByType("teacher").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteTag(tag: PhotoLogTag) {
        viewModelScope.launch { photoLogDao.deleteTag(tag) }
    }

    /** Every save path routes tags through here so none of them can forget it. */
    private suspend fun upsertTagPool(log: ClassLog) {
        log.tags.zip(listOf("studio", "level", "teacher")).forEach { (value, type) ->
            if (value.isNotBlank()) photoLogDao.upsertTag(type, value)
        }
    }

    fun insertLog(log: ClassLog) {
        viewModelScope.launch {
            dao.insert(log)
            upsertTagPool(log)
        }
    }

    fun updateLog(log: ClassLog) {
        viewModelScope.launch {
            dao.update(log)
            upsertTagPool(log)
        }
    }

    fun deleteLog(log: ClassLog) {
        viewModelScope.launch { dao.delete(log) }
    }

    fun toggleFavorite(log: ClassLog) {
        viewModelScope.launch { dao.update(log.copy(favorite = !log.favorite)) }
    }

    fun incrementViewCount(id: String) {
        viewModelScope.launch { dao.incrementViewCount(id) }
    }

    // insert 완료 후 워크아웃 fetch (새 기록용)
    fun insertAndFetchWorkout(log: ClassLog, onResult: (WorkoutInfo?) -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val hasPerms = HealthConnectManager.hasPermissions(context)
            val workout = if (hasPerms) HealthConnectManager.readWorkoutForDate(context, log.date) else null
            val gson = com.google.gson.Gson()
            val logToSave = if (workout != null) log.copy(workoutJson = gson.toJson(workout)) else log
            dao.insert(logToSave)
            upsertTagPool(logToSave)
            withContext(Dispatchers.Main) { onResult(workout) }
        }
    }

    // update 완료 후 워크아웃 fetch (기존 기록용)
    fun updateAndFetchWorkout(log: ClassLog, onResult: (WorkoutInfo?) -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val hasPerms = HealthConnectManager.hasPermissions(context)
            val workout = if (hasPerms) HealthConnectManager.readWorkoutForDate(context, log.date) else null
            val gson = com.google.gson.Gson()
            val logToSave = if (workout != null) log.copy(workoutJson = gson.toJson(workout)) else log
            dao.update(logToSave)
            upsertTagPool(logToSave)
            withContext(Dispatchers.Main) { onResult(workout) }
        }
    }

    // Health Connect 워크아웃 자동 로드 후 저장
    fun fetchAndSaveWorkout(log: ClassLog, onResult: (WorkoutInfo?) -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val hasPerms = HealthConnectManager.hasPermissions(context)
            val workout = if (hasPerms) HealthConnectManager.readWorkoutForDate(context, log.date) else null
            if (workout != null) {
                val gson = com.google.gson.Gson()
                dao.update(log.copy(workoutJson = gson.toJson(workout)))
            }
            withContext(Dispatchers.Main) { onResult(workout) }
        }
    }
}
