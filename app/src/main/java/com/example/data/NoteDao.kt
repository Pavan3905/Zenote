package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isArchived = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt <= :cutoffTime")
    suspend fun deleteExpiredNotes(cutoffTime: Long)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT DISTINCT tags FROM notes")
    fun getAllTagsFlow(): Flow<List<String>>
}
