package com.hyorita.balletlog.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BalletLogDatabase.getInstance(app)
    private val dao = db.noteDao()
    private val classLogDao = db.classLogDao()

    val notes = dao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Surfaced so NoteDetail can look up `note.linkedLogId` and navigate to
    // the actual ClassLog without each Note row having to hit the DB.
    val classLogs = classLogDao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<ClassLog>()
    )

    fun insert(note: Note) = viewModelScope.launch { dao.insert(note) }
    fun update(note: Note) = viewModelScope.launch { dao.update(note) }
    fun delete(note: Note) = viewModelScope.launch { dao.delete(note) }
    fun togglePin(note: Note) = viewModelScope.launch { dao.update(note.copy(pinned = !note.pinned)) }

    fun deleteTagFromAllNotes(tag: String) {
        viewModelScope.launch {
            val gson = Gson()
            val allNotes = dao.getAll().first()
            val notesWithTag = allNotes.filter { it.tags.contains(tag) }
            notesWithTag.forEach { note ->
                val updatedTags = note.tags.filter { it != tag }
                dao.update(
                    note.copy(
                        tagsJson = gson.toJson(updatedTags),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
