package com.hyorita.balletlog.ui.photolog

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyorita.balletlog.data.HealthConnectManager
import com.hyorita.balletlog.data.PhotoLogStorage
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.data.model.PhotoLogTag
import com.hyorita.balletlog.data.model.WorkoutInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoLogViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BalletLogDatabase.getInstance(app)
    private val dao = db.photoLogDao()

    val photoLogs = dao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val studioTags = dao.getTagsByType("studio").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val levelTags = dao.getTagsByType("level").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val teacherTags = dao.getTagsByType("teacher").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun savePhotoFromUri(uri: Uri, onSaved: (name: String?, takenDate: Long?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val n = PhotoLogStorage.saveFromUri(getApplication(), uri)
                    ?: return@withContext null to null
                val d = PhotoLogStorage.extractTakenDate(getApplication(), uri, n)
                n to d
            }
            onSaved(result.first, result.second)
        }
    }

    fun upsert(log: PhotoLog, isNew: Boolean) {
        viewModelScope.launch {
            if (isNew) dao.insert(log) else dao.update(log)
            log.tags.zip(listOf("studio", "level", "teacher")).forEach { (value, type) ->
                if (value.isNotBlank()) dao.upsertTag(type, value)
            }
        }
    }

    fun delete(log: PhotoLog) {
        viewModelScope.launch {
            dao.delete(log)
            withContext(Dispatchers.IO) {
                PhotoLogStorage.delete(getApplication(), log.photoPath)
                PhotoLogStorage.delete(getApplication(), log.filteredPhotoPath)
            }
        }
    }

    fun toggleFavorite(log: PhotoLog) {
        viewModelScope.launch {
            dao.update(log.copy(isFavorite = !log.isFavorite))
        }
    }

    /**
     * Attach a photo (already saved to PhotoLogStorage) to a workout-only
     * placeholder. Keeps the kcal / duration / HR fields intact so the
     * card stops being a placeholder but still shows the workout overlay.
     */
    fun attachPhoto(log: PhotoLog, photoFileName: String) {
        viewModelScope.launch {
            dao.update(log.copy(photoPath = photoFileName, filteredPhotoPath = null))
        }
    }

    /**
     * 1.8 "Remove Photo" — drop just the photo files and clear the path,
     * leaving the workout fields so the entry returns to placeholder state.
     */
    fun removePhoto(log: PhotoLog) {
        viewModelScope.launch {
            dao.update(log.copy(photoPath = "", filteredPhotoPath = null))
            withContext(Dispatchers.IO) {
                PhotoLogStorage.delete(getApplication(), log.photoPath)
                PhotoLogStorage.delete(getApplication(), log.filteredPhotoPath)
            }
        }
    }

    fun deleteTag(tag: PhotoLogTag) {
        viewModelScope.launch { dao.deleteTag(tag) }
    }

    fun fetchWorkoutForDate(dateMillis: Long, onResult: (WorkoutInfo?) -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val hasPerms = HealthConnectManager.hasPermissions(context)
            val workout = if (hasPerms) {
                HealthConnectManager.readWorkoutForDate(context, dateMillis)
            } else null
            withContext(Dispatchers.Main) { onResult(workout) }
        }
    }
}
