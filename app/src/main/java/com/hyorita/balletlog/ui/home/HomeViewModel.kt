package com.hyorita.balletlog.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyorita.balletlog.data.HealthConnectManager
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.WorkoutInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BalletLogDatabase.getInstance(app)
    private val dao = db.classLogDao()

    val logs = dao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertLog(log: ClassLog) {
        viewModelScope.launch { dao.insert(log) }
    }

    fun updateLog(log: ClassLog) {
        viewModelScope.launch { dao.update(log) }
    }

    fun deleteLog(log: ClassLog) {
        viewModelScope.launch { dao.delete(log) }
    }

    fun toggleFavorite(log: ClassLog) {
        viewModelScope.launch { dao.update(log.copy(favorite = !log.favorite)) }
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
