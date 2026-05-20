package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Note
import com.example.data.NoteRepository
import com.example.service.GeminiApiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Navigation / Screen state for Zenote
 */
sealed class ScreenState {
    object ListScreen : ScreenState()
    data class EditorScreen(var isEditMode: Boolean = true) : ScreenState()
    object ArchiveScreen : ScreenState()
    object TrashScreen : ScreenState()
}

/**
 * AI operational modes for content creation
 */
enum class AIOperation {
    SUMMARIZE,
    BEAUTIFY_MARKDOWN,
    EXTRACT_TASKS,
    POLISH_TONE,
    BRAINSTORM_OUTLINE,
    SUGGEST_TAGS
}

class MainViewModel(private val repository: NoteRepository) : ViewModel() {

    // Bottom Navigation or Sidebar Selection
    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.ListScreen)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // Database Streams
    val activeNotes = repository.activeNotes
    val archivedNotes = repository.archivedNotes
    val favoriteNotes = repository.favoriteNotes
    val deletedNotes = repository.deletedNotes

    init {
        purgeOldDeletedNotes()
    }

    fun purgeOldDeletedNotes() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            repository.deleteExpiredNotes(thirtyDaysAgo)
        }
    }

    // Live Unique Tags Stream
    val allTagsStream = repository.allTags
        .map { list ->
            list.flatMap { it.split(",") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filtering State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter = _selectedTagFilter.asStateFlow()

    // Toggle for showing Favorites only in list view
    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites = _showOnlyFavorites.asStateFlow()

    // Selected Note / Active Editor State
    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote = _selectedNote.asStateFlow()

    // Temp variables backing the active unsaved edit session
    private val _editorTitle = MutableStateFlow("")
    val editorTitle = _editorTitle.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent = _editorContent.asStateFlow()

    private val _editorTags = MutableStateFlow("")
    val editorTags = _editorTags.asStateFlow()

    // Theme Selector Mode (System, Light, Dark)
    private val _themeMode = MutableStateFlow("System")
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    private val _isGridView = MutableStateFlow(false)
    val isGridView = _isGridView.asStateFlow()

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    // AI Assist States
    private val _isAILoading = MutableStateFlow(false)
    val isAILoading = _isAILoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError = _aiError.asStateFlow()

    private val _aiSuggestedTags = MutableStateFlow<List<String>>(emptyList())
    val aiSuggestedTags = _aiSuggestedTags.asStateFlow()

    private val _selectedCalendarDate = MutableStateFlow<String?>(null)
    val selectedCalendarDate = _selectedCalendarDate.asStateFlow()

    fun selectCalendarDate(dateStr: String?) {
        _selectedCalendarDate.value = dateStr
    }

    // Final UI List computed with search query / tag combinations
    val filteredNotes = combine(
        activeNotes,
        _searchQuery,
        _selectedTagFilter,
        _showOnlyFavorites,
        _selectedCalendarDate
    ) { notes, query, tag, favs, calendarDate ->
        notes.asSequence()
            .filter { note ->
                val matchesQuery = note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
                val matchesTag = tag == null || note.getTagsList().contains(tag)
                val matchesFav = !favs || note.isFavorite
                val matchesCalendar = if (calendarDate == null) {
                    true
                } else {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val noteDateStr = sdf.format(java.util.Date(note.updatedAt))
                    noteDateStr == calendarDate
                }
                matchesQuery && matchesTag && matchesFav && matchesCalendar
            }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var saveJob: kotlinx.coroutines.Job? = null

    fun selectScreen(state: ScreenState) {
        saveCurrentEditorContentImmediately()
        _screenState.value = state
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun toggleFavoritesFilter() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }

    // --- Core Note Actions ---

    fun openNote(note: Note) {
        _selectedNote.value = note
        _editorTitle.value = note.title
        _editorContent.value = note.content
        _editorTags.value = note.tags
        _aiSuggestedTags.value = emptyList()
        _aiError.value = null
        _screenState.value = ScreenState.EditorScreen(isEditMode = false)
    }

    fun createNewNote() {
        viewModelScope.launch {
            val newNote = Note(
                title = "",
                content = "",
                tags = ""
            )
            val id = repository.insert(newNote)
            val created = repository.getNoteById(id.toInt())
            if (created != null) {
                openNote(created)
                // Go straight to edit mode for seamless user workflow
                _screenState.value = ScreenState.EditorScreen(isEditMode = true)
            }
        }
    }

    fun updateEditorContent(content: String) {
        _editorContent.value = content
        // Also automatically auto-save changes if a note is selected so user never loses progress!
        saveCurrentEditorContent()
    }

    fun updateEditorTitle(title: String) {
        _editorTitle.value = title
        saveCurrentEditorContent()
    }

    fun updateEditorTags(tags: String) {
        _editorTags.value = tags
        saveCurrentEditorContent()
    }

    fun addTagToEditor(tag: String) {
        val current = _editorTags.value.trim()
        val tagsList = if (current.isEmpty()) mutableListOf() else current.split(",").map { it.trim() }.toMutableList()
        if (!tagsList.contains(tag)) {
            tagsList.add(tag)
            updateEditorTags(tagsList.joinToString(", "))
        }
    }

    private fun saveCurrentEditorContent() {
        val current = _selectedNote.value ?: return
        val updated = current.copy(
            title = _editorTitle.value,
            content = _editorContent.value,
            tags = _editorTags.value,
            updatedAt = System.currentTimeMillis()
        )
        _selectedNote.value = updated

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            repository.update(updated)
        }
    }

    fun saveCurrentEditorContentImmediately() {
        val current = _selectedNote.value ?: return
        saveJob?.cancel()
        val updated = current.copy(
            title = _editorTitle.value,
            content = _editorContent.value,
            tags = _editorTags.value,
            updatedAt = System.currentTimeMillis()
        )
        _selectedNote.value = updated
        viewModelScope.launch {
            repository.update(updated)
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isFavorite = !note.isFavorite)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
            }
            repository.update(updated)
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isArchived = !note.isArchived)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
                // Close editor on archive
                _screenState.value = ScreenState.ListScreen
            }
            repository.update(updated)
        }
    }

    fun deleteNote(note: Note) {
        trashNote(note)
    }

    fun trashNote(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = null
                _screenState.value = ScreenState.ListScreen
            }
            repository.update(updated)
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isDeleted = false, deletedAt = null)
            repository.update(updated)
        }
    }

    fun deleteNotePermanently(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = null
                _screenState.value = ScreenState.ListScreen
            }
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            // Delete all currently deleted notes from SQLite completely
            val currentDeleted = repository.deletedNotes.firstOrNull() ?: emptyList()
            currentDeleted.forEach {
                repository.delete(it)
            }
        }
    }

    fun setNoteReminder(note: Note, timestamp: Long?) {
        viewModelScope.launch {
            val updated = note.copy(reminderTime = timestamp)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
                _editorTitle.value = updated.title // ensure synced
            }
            repository.update(updated)
        }
    }

    fun updateNoteNotebook(note: Note, notebookName: String) {
        viewModelScope.launch {
            val updated = note.copy(notebook = notebookName)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
            }
            repository.update(updated)
        }
    }

    fun updateNoteTitleAndNotebook(note: Note, title: String, notebookName: String) {
        viewModelScope.launch {
            val updated = note.copy(
                title = title,
                notebook = notebookName,
                updatedAt = System.currentTimeMillis()
            )
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
                _editorTitle.value = title
            }
            repository.update(updated)
        }
    }

    // --- AI Operations using direct Gemini Rest Client ---

    fun runAIEngine(operation: AIOperation, promptInput: String = "") {
        val currentContent = _editorContent.value
        val currentTitle = _editorTitle.value

        _isAILoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            try {
                val systemInstruction = """
                    You are Zenote AI, an elite note-taking co-writer integrated inside a professional Markdown note editor.
                    Your goal is to assist the user in polishing, structuring, summarizing, and improving notes.
                    Output raw notes ONLY in structured Standard Markdown. 
                    Do NOT include pleasantries, introductory chat, or conversational text like "Here is your output...". 
                    Respond immediately and purely with the requested contents.
                """.trimIndent()

                val prompt = when (operation) {
                    AIOperation.SUMMARIZE -> {
                        """
                            Read the following Note titled "$currentTitle" and generate a beautiful, modern, minimalist, bullet-pointed summary.
                            Insert an elegant divider, then append the summary formatted under a "### ⚡ Cosmic Summary" heading.
                            
                            === Current Note ===
                            $currentContent
                        """.trimIndent()
                    }
                    AIOperation.BEAUTIFY_MARKDOWN -> {
                        """
                            Transform and beatify the markdown structure of the note below. 
                            Ensure correct usage of headers (#, ##, ###), bullet points, bolding (**bold**), subheadings, clean code blocks where applicable, and horizontal lines (---).
                            Clean up syntax issues and spacing to make it extremely premium, readable and professional.
                            Keep the exact core content and message intact, just formatting it beautifully.
                            
                            === Note Title ===
                            $currentTitle
                            
                            === Unformatted Note Content ===
                            $currentContent
                        """.trimIndent()
                    }
                    AIOperation.EXTRACT_TASKS -> {
                        """
                            Extract all direct and implied action items, checklists, or steps from this note.
                            Present them purely as a standard list of checkable items with checkboxes (- [ ] task detail).
                            If there are already checkboxes, list them and add new ones found in the text.
                            Format this neatly under a "### ☑️ AI Generated To-Do List" section, with a thin divider (---).
                            
                            === Note Content ===
                            $currentContent
                        """.trimIndent()
                    }
                    AIOperation.POLISH_TONE -> {
                        """
                            Rewrite or polish the professional tone of this note to be highly captivating, modern and clean.
                            Maintain the exact meaning but elevate the writing quality. Ensure formatting stays in clean markdown. 
                            Selected polish style: "$promptInput"
                            
                            === Note Title ===
                            $currentTitle
                            
                            === Note Content ===
                            $currentContent
                        """.trimIndent()
                    }
                    AIOperation.BRAINSTORM_OUTLINE -> {
                        """
                            The user wants to write a note titled: "$currentTitle".
                            Generate a highly valuable, structured, advanced outline and creative template in Markdown to kickstart their writing. Include sections and ideas to explore.
                            
                            === Additional User Prompt/Context ===
                            $promptInput
                        """.trimIndent()
                    }
                    AIOperation.SUGGEST_TAGS -> {
                        """
                            Read the note contents below and generate 4-6 contextual, short, search-friendly tags (one to two words per tag) representing the topics.
                            Respond strictly with a pure JSON array of strings, e.g. ["technology", "coding", "productivity"]. No other text output.
                            
                            === Note Content ===
                            $currentContent
                        """.trimIndent()
                    }
                }

                val result = GeminiApiClient.generate(systemInstruction, prompt)

                when (operation) {
                    AIOperation.SUGGEST_TAGS -> {
                        // Extract array elements from JSON
                        val regex = """"(.*?)"""".toRegex()
                        val matches = regex.findAll(result).map { it.groupValues[1] }.toList()
                        _aiSuggestedTags.value = matches.ifEmpty { listOf("SmartNotes", "AI") }
                    }
                    AIOperation.SUMMARIZE, AIOperation.EXTRACT_TASKS -> {
                        // Append actions/summaries with an elegant visual divider
                        val appended = "$currentContent\n\n---\n$result"
                        updateEditorContent(appended)
                    }
                    AIOperation.BEAUTIFY_MARKDOWN, AIOperation.POLISH_TONE, AIOperation.BRAINSTORM_OUTLINE -> {
                        // Overwrite content with elevated draft
                        updateEditorContent(result)
                    }
                }
            } catch (e: Exception) {
                _aiError.value = e.message ?: "An unexpected error occurred during AI processing."
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun clearAIStates() {
        _aiSuggestedTags.value = emptyList()
        _aiError.value = null
        _isAILoading.value = false
    }
}

/**
 * Custom Factory to wire dependencies into ViewModel
 */
class MainViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
