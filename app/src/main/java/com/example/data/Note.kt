package com.example.data

import androidx.room.*

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: String = "",       // Comma-separated list of tags
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val reminderTime: Long? = null,
    val notebook: String = ""
) {
    // Helper to get formatted tags as a list
    fun getTagsList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
