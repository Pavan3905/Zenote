package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val activeNotes: Flow<List<Note>> = noteDao.getAllActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val favoriteNotes: Flow<List<Note>> = noteDao.getFavoriteNotes()
    val deletedNotes: Flow<List<Note>> = noteDao.getDeletedNotes()

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    suspend fun insert(note: Note): Long = noteDao.insertNote(note)

    suspend fun update(note: Note) = noteDao.updateNote(note)

    suspend fun delete(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteExpiredNotes(cutoffTime: Long) = noteDao.deleteExpiredNotes(cutoffTime)

    val allTags: Flow<List<String>> = noteDao.getAllTagsFlow()
}
