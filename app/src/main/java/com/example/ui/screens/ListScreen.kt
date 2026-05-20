package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.filteredNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val allTags by viewModel.allTagsStream.collectAsState()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val fabBg = if (isDark) Color(0xFF2E293E) else Color(0xFFD3E3FD)
    val fabIconTint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF041E49)

    // Sidebar Navigation drawer actions
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Bottom Navigation tab: 0 = Notes Page Segments, 1 = Calendar Schedule Planner
    var currentBottomTab by remember { mutableStateOf(0) }

    // Calendar month tracking state
    var currentMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDateStr by viewModel.selectedCalendarDate.collectAsState()

    // Extract active notes and compute days containing updates/reminders
    val allNotesRaw by viewModel.activeNotes.collectAsState(initial = emptyList())
    val daysWithNotesStr = remember(allNotesRaw) {
        val sdfText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        allNotesRaw.map { sdfText.format(Date(it.updatedAt)) }.toSet()
    }

    val daysWithRemindersStr = remember(allNotesRaw) {
        val sdfText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        allNotesRaw.filter { it.reminderTime != null }.map { sdfText.format(Date(it.reminderTime!!)) }.toSet()
    }

    val context = LocalContext.current

    // Reminders & Renaming popup dialog states
    var noteForReminder by remember { mutableStateOf<Note?>(null) }
    var noteToRenameMove by remember { mutableStateOf<Note?>(null) }

    // Grouping notes by Notebook Organization structure dynamically
    val notebookSegments = remember(notes) {
        notes.groupBy { it.notebook.trim() }
    }

    // Default expanded states for notebooks
    val expandedStates = remember(notebookSegments) {
        val map = mutableStateMapOf<String, Boolean>()
        notebookSegments.keys.forEach {
            map[it] = true
        }
        map
    }

    // Horizontal Recents Section
    val recents = remember(allNotesRaw) {
        allNotesRaw.sortedByDescending { it.updatedAt }.take(5)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(285.dp)
            ) {
                Spacer(modifier = Modifier.height(18.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Zenote",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Minimalist Workspace",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "WORKSPACE STATUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${allNotesRaw.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("Total Pages", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val favCount = allNotesRaw.count { it.isFavorite }
                                Text("$favCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFCC00))
                                Text("Starred", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        text = "QUICK LINKS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 10.dp, bottom = 6.dp)
                    )

                    // Link to Archives page
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Archive, null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Archived Pages", fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectScreen(ScreenState.ArchiveScreen)
                        },
                        modifier = Modifier.height(42.dp)
                    )

                    // Link to Recycle Bin page
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Recycle Bin", fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectScreen(ScreenState.TrashScreen)
                        },
                        modifier = Modifier.height(42.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 10.dp))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PREFERENCES & SUPPORT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 10.dp, bottom = 6.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp)) },
                        label = { Text("App Preferences", fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showSettingsDialog = true
                        },
                        modifier = Modifier.height(42.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                        label = { Text("How to Use (Help)", fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showHelpDialog = true
                        },
                        modifier = Modifier.height(42.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { currentBottomTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentBottomTab == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (currentBottomTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = "Active Workspace", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Workspace Pages", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { currentBottomTab = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentBottomTab == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (currentBottomTab == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Schedules and Reminders", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Calendar Planner", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentBottomTab == 0) {
                    FloatingActionButton(
                        onClick = { viewModel.createNewNote() },
                        modifier = Modifier.testTag("create_note_button").padding(bottom = 6.dp),
                        containerColor = fabBg,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Create New Page", tint = fabIconTint, modifier = Modifier.size(26.dp))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp)
            ) {
                // Sleek, Space-Optimized Top Title Row
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "Zenote",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.3).sp
                            )
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Help Guide", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Conditionally display tab elements
                if (currentBottomTab == 0) {
                    // Search Bar
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search pages...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("search_field")
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Elegant modernized Filter section
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = showOnlyFavorites,
                                onClick = { viewModel.toggleFavoritesFilter() },
                                label = { Text("⭐ Starred", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedTagFilter == null,
                                onClick = { viewModel.selectTagFilter(null) },
                                label = { Text("All Workspace", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                            )
                        }

                        items(allTags) { tag ->
                            FilterChip(
                                selected = selectedTagFilter == tag,
                                onClick = { viewModel.selectTagFilter(if (selectedTagFilter == tag) null else tag) },
                                label = { Text("#$tag", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Recents scrolling cards zone
                    if (recents.isNotEmpty() && selectedTagFilter == null && searchQuery.isEmpty()) {
                        Text(
                            text = "Recents",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            items(recents) { note ->
                                Card(
                                    modifier = Modifier
                                        .width(170.dp)
                                        .clickable { viewModel.openNote(note) }
                                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = note.title.ifBlank { "Untitled" },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = note.content.take(55).replace("\n", " ").trim(),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (note.notebook.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = note.notebook,
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (notes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = "Empty", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("A blank canvas awaits", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        // Notebook Segments Organised accordion tree list
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            notebookSegments.forEach { (notebookName, segmentNotes) ->
                                item {
                                    val isExpanded = expandedStates[notebookName] ?: true
                                    val displayName = if (notebookName.isBlank()) "📁 General Notes" else "📁 $notebookName"

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedStates[notebookName] = !isExpanded }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = displayName,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                                    )
                                                )
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Toggle Accordion",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            if (isExpanded) {
                                                Column(
                                                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    segmentNotes.forEach { note ->
                                                        NoteCard(
                                                            note = note,
                                                            onClick = { viewModel.openNote(note) },
                                                            onFavoriteToggle = { viewModel.toggleFavorite(note) },
                                                            onDelete = { viewModel.deleteNote(note) },
                                                            onArchive = { viewModel.toggleArchive(note) },
                                                            onRenameMove = { noteToRenameMove = note },
                                                            onSetReminder = { noteForReminder = note },
                                                            isDense = isGridView
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // Bottom Tab 1: Calendar View with reminder summary focus
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sdfMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
                                Text(
                                    text = sdfMonth.format(currentMonthCalendar.time),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val prevCal = currentMonthCalendar.clone() as Calendar
                                            prevCal.add(Calendar.MONTH, -1)
                                            currentMonthCalendar = prevCal
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, "Prev", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val nextCal = currentMonthCalendar.clone() as Calendar
                                            nextCal.add(Calendar.MONTH, 1)
                                            currentMonthCalendar = nextCal
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowForward, "Next", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                                    Text(
                                        text = dayLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.width(26.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val tempCal = currentMonthCalendar.clone() as Calendar
                            tempCal.set(Calendar.DAY_OF_MONTH, 1)
                            val startDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
                            val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                            val cells = mutableListOf<Int?>()
                            for (i in 0 until startDayOfWeek) { cells.add(null) }
                            for (d in 1..maxDay) { cells.add(d) }

                            val rowChunks = cells.chunked(7)
                            rowChunks.forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    week.forEach { dayNum ->
                                        if (dayNum == null) {
                                            Spacer(modifier = Modifier.size(26.dp))
                                        } else {
                                            val cellYear = currentMonthCalendar.get(Calendar.YEAR)
                                            val cellMonth = currentMonthCalendar.get(Calendar.MONTH) + 1
                                            val dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", cellYear, cellMonth, dayNum)
                                            val hasNotesOnDate = daysWithNotesStr.contains(dateKey)
                                            val hasRemindersOnDate = daysWithRemindersStr.contains(dateKey)
                                            val isCellSelected = selectedDateStr == dateKey

                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isCellSelected) MaterialTheme.colorScheme.primary
                                                        else if (hasRemindersOnDate) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable {
                                                        if (isCellSelected) {
                                                            viewModel.selectCalendarDate(null)
                                                        } else {
                                                            viewModel.selectCalendarDate(dateKey)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                    Text(
                                                        text = "$dayNum",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isCellSelected || hasNotesOnDate || hasRemindersOnDate) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isCellSelected) MaterialTheme.colorScheme.onPrimary
                                                                else if (hasRemindersOnDate) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (hasNotesOnDate) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(3.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isCellSelected) MaterialTheme.colorScheme.onPrimary
                                                                    else MaterialTheme.colorScheme.secondary
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (week.size < 7) {
                                        for (i in 0 until (7 - week.size)) { Spacer(modifier = Modifier.size(26.dp)) }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notes containing edits or reminders matching this selected slot
                    val activeTargetNotes = remember(selectedDateStr, allNotesRaw) {
                        if (selectedDateStr == null) {
                            allNotesRaw.filter { it.reminderTime != null } // Default view active reminders of all schedule pages
                        } else {
                            val sdfText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            allNotesRaw.filter {
                                sdfText.format(Date(it.updatedAt)) == selectedDateStr ||
                                        (it.reminderTime != null && sdfText.format(Date(it.reminderTime)) == selectedDateStr)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDateStr != null) "Schedules for $selectedDateStr" else "Upcoming Active Reminders",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        if (selectedDateStr != null) {
                            TextButton(onClick = { viewModel.selectCalendarDate(null) }) {
                                Text("Show all upcoming", fontSize = 11.sp)
                            }
                        }
                    }

                    if (activeTargetNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = "None", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), modifier = Modifier.size(46.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No logged schedules", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(activeTargetNotes) { note ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openNote(note) }
                                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(note.title.ifBlank { "Untitled" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            if (note.reminderTime != null) {
                                                val sdfReminder = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(sdfReminder.format(Date(note.reminderTime)), fontSize = 9.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(note.content.take(80).replace("\n", " "), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Preferences Screen dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("App Preferences")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Theme Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Light", "Dark", "System").forEach { mode ->
                                FilterChip(
                                    selected = (mode == "Light" && themeMode == "Light") ||
                                            (mode == "Dark" && themeMode == "Dark") ||
                                            (mode == "System" && themeMode == "System"),
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(if (mode == "System") "System Default" else mode) }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Explorer Layout", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(onClick = { viewModel.toggleGridView() }) {
                            Icon(if (isGridView) Icons.Default.ViewStream else Icons.Default.GridView, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isGridView) "Switch to List Cards" else "Switch to Grid Cards")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) { Text("Close") }
            }
        )
    }

    // Modern Simplified Help guide Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Help Guide", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("How to Use (Documentation)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Column {
                        Text("⌨️ List Auto-Formatting", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("• Type '1. ' or '- ' and press Enter to auto-generate lists.\n• Press Enter twice to stop formatting.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column {
                        Text("✍️ Live Markdown Guide", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "• Bold: **Bold Text**\n• Italics: *Italic Text*\n• Headings: # Header 1, ## Header 2\n• Blockquotes: > Quote segments",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        Text("⏰ Schedulers & Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("• Tap the vertical menu on any page segment card and select 'Set Reminder' to anchor to the planner.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) { Text("Close") }
            }
        )
    }

    // Native dialog picker launcher for adding page reminders
    if (noteForReminder != null) {
        val note = noteForReminder!!
        val calendar = Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val timePickerDialog = android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        viewModel.setNoteReminder(note, calendar.timeInMillis)
                        noteForReminder = null
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        DisposableEffect(note) {
            datePickerDialog.show()
            onDispose { datePickerDialog.dismiss() }
        }
    }

    // Dialog picker for renaming and notebook moves
    if (noteToRenameMove != null) {
        val note = noteToRenameMove!!
        var tempTitle by remember { mutableStateOf(note.title) }
        var tempNotebook by remember { mutableStateOf(note.notebook) }

        AlertDialog(
            onDismissRequest = { noteToRenameMove = null },
            title = { Text("Rename & Move Page Unit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Rename title or re-allocate page unit under modern notebooks grouping.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempNotebook,
                        onValueChange = { tempNotebook = it },
                        label = { Text("Modern Notebook Group") },
                        placeholder = { Text("e.g. Work, Personal, Academic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateNoteTitleAndNotebook(note, tempTitle, tempNotebook)
                        noteToRenameMove = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToRenameMove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onRenameMove: () -> Unit,
    onSetReminder: () -> Unit,
    isDense: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dateString = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    val wordsCount = remember(note.content) {
        note.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }

    val readingTimeStr = remember(wordsCount) {
        val minutes = maxOf(1, wordsCount / 180)
        "$minutes min read"
    }

    val textSnippet = remember(note.content) {
        val clean = note.content
            .replace("#", "")
            .replace("*", "")
            .replace("---", "")
            .replace("`", "")
            .trim()
        if (isDense) clean.take(55) else clean.take(110)
    }

    val cardShape = if (isDense) RoundedCornerShape(12.dp) else RoundedCornerShape(16.dp)
    val cardPadding = if (isDense) 10.dp else 12.dp
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}")
            .clickable { onClick() }
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape = cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = if (isDense) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (note.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(if (isDense) 26.dp else 32.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) Color(0xFFFFCC00) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(if (isDense) 15.dp else 18.dp)
                        )
                    }

                    var showCardMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showCardMenu = true },
                            modifier = Modifier.size(if (isDense) 26.dp else 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(if (isDense) 15.dp else 18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename / Move") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showCardMenu = false
                                    onRenameMove()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set Reminder") },
                                leadingIcon = { Icon(Icons.Default.Alarm, null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showCardMenu = false
                                    onSetReminder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share PDF (.pdf)") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showCardMenu = false
                                    com.example.ui.screens.shareNoteAsPdf(context, note.title.ifBlank { "Untitled Note" }, note.content)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Markdown (.md)") },
                                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showCardMenu = false
                                    com.example.ui.screens.shareMarkdownFile(context, note.title.ifBlank { "Untitled Note" }, note.content)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Archive note") },
                                leadingIcon = { Icon(Icons.Default.Archive, null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showCardMenu = false
                                    onArchive()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete note") },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showCardMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isDense) 2.dp else 4.dp))

            Text(
                text = textSnippet.ifBlank { "Empty segment content..." },
                style = if (isDense) MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp)
                        else MaterialTheme.typography.bodyMedium.copy(lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = if (isDense) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(if (isDense) 6.dp else 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = dateString, style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isDense) 9.sp else 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)))
                    Text(text = readingTimeStr, style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isDense) 9.sp else 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    // Show reminders indicator
                    if (note.reminderTime != null) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Alarm, "Reminder active", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(10.dp))
                        }
                    }

                    // Show custom Tag Label Label
                    note.getTagsList().take(if (isDense) 1 else 2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (isDense) 8.sp else 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
