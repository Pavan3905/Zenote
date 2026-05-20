package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.AIOperation
import com.example.ui.components.MarkdownRender
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Share
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas

enum class EditorViewMode {
    WRITE,
    PREVIEW
}

class MarkdownVisualTransformation(
    private val primaryColor: Color,
    private val secondaryColor: Color,
    private val tertiaryColor: Color,
    private val onSurfaceVariant: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val annotated = parseLiveMarkdown(rawText, primaryColor, secondaryColor, tertiaryColor, onSurfaceVariant)
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

fun parseLiveMarkdown(
    text: String,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onSurfaceVariant: Color
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        val textLen = text.length
        
        fun safeAddStyle(style: SpanStyle, start: Int, end: Int) {
            val s = start.coerceIn(0, textLen)
            val e = end.coerceIn(0, textLen)
            if (s < e) {
                addStyle(style, s, e)
            }
        }

        val lines = text.split("\n")
        var currentOffset = 0
        
        for (line in lines) {
            val lineLength = line.length
            val trimmedLine = line.trimStart()
            val leadingSpaces = line.length - trimmedLine.length
            
            if (trimmedLine.startsWith("# ")) {
                val start = currentOffset + leadingSpaces
                safeAddStyle(
                    SpanStyle(color = onSurfaceVariant.copy(alpha = 0.35f), fontWeight = FontWeight.Normal),
                    start,
                    start + 2
                )
                safeAddStyle(
                    SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 22.sp),
                    start + 2,
                    currentOffset + lineLength
                )
            } else if (trimmedLine.startsWith("## ")) {
                val start = currentOffset + leadingSpaces
                safeAddStyle(
                    SpanStyle(color = onSurfaceVariant.copy(alpha = 0.35f), fontWeight = FontWeight.Normal),
                    start,
                    start + 3
                )
                safeAddStyle(
                    SpanStyle(color = secondaryColor, fontWeight = FontWeight.Bold, fontSize = 19.sp),
                    start + 3,
                    currentOffset + lineLength
                )
            } else if (trimmedLine.startsWith("### ")) {
                val start = currentOffset + leadingSpaces
                safeAddStyle(
                    SpanStyle(color = onSurfaceVariant.copy(alpha = 0.35f), fontWeight = FontWeight.Normal),
                    start,
                    start + 4
                )
                safeAddStyle(
                    SpanStyle(color = tertiaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    start + 4,
                    currentOffset + lineLength
                )
            }
            else if (trimmedLine.startsWith("> ")) {
                val start = currentOffset + leadingSpaces
                safeAddStyle(
                    SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                    start,
                    start + 2
                )
                safeAddStyle(
                    SpanStyle(color = onSurfaceVariant, fontStyle = FontStyle.Italic),
                    start + 2,
                    currentOffset + lineLength
                )
            }
            else if (trimmedLine.startsWith("- [ ] ")) {
                val start = currentOffset + leadingSpaces
                safeAddStyle(
                    SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    start,
                    start + 5
                )
            } else if (trimmedLine.startsWith("- [x] ")) {
                val start = currentOffset + leadingSpaces
                safeAddStyle(
                    SpanStyle(color = primaryColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    start,
                    start + 5
                )
                safeAddStyle(
                    SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, color = onSurfaceVariant.copy(alpha = 0.5f)),
                    start + 5,
                    currentOffset + lineLength
                )
            }
            else if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")) {
                val start = currentOffset + leadingSpaces
                safeAddStyle(
                    SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                    start,
                    start + 2
                )
            }
            
            var inlineIdx = 0
            while (inlineIdx < lineLength) {
                if (inlineIdx + 1 < lineLength && line[inlineIdx] == '*' && line[inlineIdx+1] == '*') {
                    val closing = line.indexOf("**", inlineIdx + 2)
                    if (closing != -1) {
                        safeAddStyle(
                            SpanStyle(color = onSurfaceVariant.copy(alpha = 0.3f)),
                            currentOffset + inlineIdx,
                            currentOffset + inlineIdx + 2
                        )
                        safeAddStyle(
                            SpanStyle(color = onSurfaceVariant.copy(alpha = 0.3f)),
                            currentOffset + closing,
                            currentOffset + closing + 2
                        )
                        safeAddStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            currentOffset + inlineIdx + 2,
                            currentOffset + closing
                        )
                        inlineIdx = closing + 2
                        continue
                    }
                }
                
                if (inlineIdx + 1 < lineLength && line[inlineIdx] == '~' && line[inlineIdx+1] == '~') {
                    val closing = line.indexOf("~~", inlineIdx + 2)
                    if (closing != -1) {
                        safeAddStyle(
                            SpanStyle(color = onSurfaceVariant.copy(alpha = 0.3f)),
                            currentOffset + inlineIdx,
                            currentOffset + inlineIdx + 2
                        )
                        safeAddStyle(
                            SpanStyle(color = onSurfaceVariant.copy(alpha = 0.3f)),
                            currentOffset + closing,
                            currentOffset + closing + 2
                        )
                        safeAddStyle(
                            SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                            currentOffset + inlineIdx + 2,
                            currentOffset + closing
                        )
                        inlineIdx = closing + 2
                        continue
                    }
                }

                if (line[inlineIdx] == '*' || line[inlineIdx] == '_') {
                    val mark = line[inlineIdx].toString()
                    val closing = line.indexOf(mark, inlineIdx + 1)
                    if (closing != -1) {
                        safeAddStyle(
                            SpanStyle(color = onSurfaceVariant.copy(alpha = 0.3f)),
                            currentOffset + inlineIdx,
                            currentOffset + inlineIdx + 1
                        )
                        safeAddStyle(
                            SpanStyle(color = onSurfaceVariant.copy(alpha = 0.3f)),
                            currentOffset + closing,
                            currentOffset + closing + 1
                        )
                        safeAddStyle(
                            SpanStyle(fontStyle = FontStyle.Italic),
                            currentOffset + inlineIdx + 1,
                            currentOffset + closing
                        )
                        inlineIdx = closing + 1
                        continue
                    }
                }

                if (line[inlineIdx] == '`') {
                    val closing = line.indexOf('`', inlineIdx + 1)
                    if (closing != -1) {
                        safeAddStyle(
                            SpanStyle(color = primaryColor.copy(alpha = 0.4f)),
                            currentOffset + inlineIdx,
                            currentOffset + inlineIdx + 1
                        )
                        safeAddStyle(
                            SpanStyle(color = primaryColor.copy(alpha = 0.4f)),
                            currentOffset + closing,
                            currentOffset + closing + 1
                        )
                        safeAddStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = primaryColor.copy(alpha = 0.08f),
                                color = primaryColor,
                                fontSize = 14.sp
                            ),
                            currentOffset + inlineIdx + 1,
                            currentOffset + closing
                        )
                        inlineIdx = closing + 1
                        continue
                    }
                }

                inlineIdx++
            }
            
            currentOffset += lineLength + 1
        }
    }
}

fun handleListContinuation(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue? {
    val oldText = oldValue.text
    val newText = newValue.text

    // We only trigger when text has increased (usually by 1) and a newline '\n' was inserted at the cursor
    if (newText.length <= oldText.length) return null

    val caret = newValue.selection.end
    if (caret <= 0) return null

    // The character just before the current cursor must be '\n'
    if (newText[caret - 1] != '\n') return null

    // Find the starts of the completed line
    // The newline just typed is at index caret - 1.
    val lineEndIdx = caret - 1
    val lineStartIdx = newText.lastIndexOf('\n', lineEndIdx - 1) + 1
    val completedLine = newText.substring(lineStartIdx, lineEndIdx)

    // Analyze the completedLine to see if it matches any pattern
    // Pattern 1: Checked/Unchecked lists "- [ ] " or "- [x] "
    val checkboxRegex = Regex("""^(\s*-\s*\[(?:x|X|\s)\]\s+)(.*)$""")
    // Pattern 2: Bullet lists "- " or "* " or "• "
    val bulletRegex = Regex("""^(\s*(?:-\s+|\*\s+|•\s+))(.*)$""")
    // Pattern 3: Numbered lists "\d+\. "
    val numberedRegex = Regex("""^(\s*(\d+)\.\s+)(.*)$""")

    val matchCheckbox = checkboxRegex.matchEntire(completedLine)
    val matchBullet = bulletRegex.matchEntire(completedLine)
    val matchNumbered = numberedRegex.matchEntire(completedLine)

    if (matchCheckbox != null) {
        val prefix = matchCheckbox.groupValues[1]
        val content = matchCheckbox.groupValues[2].trim()
        if (content.isEmpty()) {
            val updatedText = newText.substring(0, lineStartIdx) + newText.substring(lineEndIdx)
            val newCaret = lineStartIdx + 1
            return TextFieldValue(
                text = updatedText,
                selection = TextRange(newCaret.coerceIn(0, updatedText.length))
            )
        } else {
            val nextPrefix = "- [ ] "
            val updatedText = newText.substring(0, caret) + nextPrefix + newText.substring(caret)
            val newCaret = caret + nextPrefix.length
            return TextFieldValue(
                text = updatedText,
                selection = TextRange(newCaret.coerceIn(0, updatedText.length))
            )
        }
    } else if (matchBullet != null) {
        val prefix = matchBullet.groupValues[1]
        val content = matchBullet.groupValues[2].trim()
        if (content.isEmpty()) {
            val updatedText = newText.substring(0, lineStartIdx) + newText.substring(lineEndIdx)
            val newCaret = lineStartIdx + 1
            return TextFieldValue(
                text = updatedText,
                selection = TextRange(newCaret.coerceIn(0, updatedText.length))
            )
        } else {
            val updatedText = newText.substring(0, caret) + prefix + newText.substring(caret)
            val newCaret = caret + prefix.length
            return TextFieldValue(
                text = updatedText,
                selection = TextRange(newCaret.coerceIn(0, updatedText.length))
            )
        }
    } else if (matchNumbered != null) {
        val prefix = matchNumbered.groupValues[1]
        val numStr = matchNumbered.groupValues[2]
        val content = matchNumbered.groupValues[3].trim()
        if (content.isEmpty()) {
            val updatedText = newText.substring(0, lineStartIdx) + newText.substring(lineEndIdx)
            val newCaret = lineStartIdx + 1
            return TextFieldValue(
                text = updatedText,
                selection = TextRange(newCaret.coerceIn(0, updatedText.length))
            )
        } else {
            val num = numStr.toIntOrNull() ?: 1
            val nextPrefix = "${num + 1}. "
            val updatedText = newText.substring(0, caret) + nextPrefix + newText.substring(caret)
            val newCaret = caret + nextPrefix.length
            return TextFieldValue(
                text = updatedText,
                selection = TextRange(newCaret.coerceIn(0, updatedText.length))
            )
        }
    }

    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val screenState by viewModel.screenState.collectAsState()
    val isEditMode = (screenState as? ScreenState.EditorScreen)?.isEditMode ?: true

    // Text & Fields updates
    val editorTitle by viewModel.editorTitle.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val editorTags by viewModel.editorTags.collectAsState()

    // Gemini AI states integration
    val isAILoading by viewModel.isAILoading.collectAsState()
    val aiError by viewModel.aiError.collectAsState()
    val aiSuggestedTags by viewModel.aiSuggestedTags.collectAsState()

    var showAiBottomSheet by remember { mutableStateOf(false) }
    var customAiPrompt by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Professional") }

    var bodyTextFieldValue by remember {
        mutableStateOf(TextFieldValue(text = editorContent))
    }

    LaunchedEffect(editorContent) {
        if (bodyTextFieldValue.text != editorContent) {
            bodyTextFieldValue = bodyTextFieldValue.copy(
                text = editorContent,
                selection = TextRange(editorContent.length)
            )
        }
    }

    var activeViewMode by remember { mutableStateOf(EditorViewMode.WRITE) }
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val markdownVisualTransformation = remember(primaryColor, secondaryColor, tertiaryColor, onSurfaceVariant) {
        MarkdownVisualTransformation(primaryColor, secondaryColor, tertiaryColor, onSurfaceVariant)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.text.BasicTextField(
                        value = editorTitle,
                        onValueChange = { viewModel.updateEditorTitle(it) },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (editorTitle.isEmpty()) {
                                Text(
                                    "Title",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("appbar_title_input")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectScreen(ScreenState.ListScreen) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Sparkling AI Assistant triggers Panel
                    IconButton(
                        onClick = { showAiBottomSheet = !showAiBottomSheet },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant Sparks",
                            tint = if (showAiBottomSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        )
                    }

                    // Single Live toggle for Edit & Preview modes
                    IconButton(
                        onClick = { 
                            activeViewMode = if (activeViewMode == EditorViewMode.WRITE) EditorViewMode.PREVIEW else EditorViewMode.WRITE 
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (activeViewMode == EditorViewMode.WRITE) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = if (activeViewMode == EditorViewMode.WRITE) "Show Preview" else "Show Editor",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Quick Favorite Star Toggle
                    val selectedNote by viewModel.selectedNote.collectAsState()
                    selectedNote?.let { note ->
                        IconButton(onClick = { viewModel.toggleFavorite(note) }) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) Color(0xFFFFCC00) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Sharing and Exporting Dropdown Option
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share or Export Note",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                text = { Text("Share Plain Text") },
                                onClick = {
                                    showShareMenu = false
                                    sharePlainText(context, editorTitle.ifBlank { "Untitled Note" }, editorContent)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Download, null) },
                                text = { Text("Export Markdown (.md)") },
                                onClick = {
                                    showShareMenu = false
                                    shareMarkdownFile(context, editorTitle.ifBlank { "Untitled Note" }, editorContent)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                text = { Text("Share as PDF (.pdf)") },
                                onClick = {
                                    showShareMenu = false
                                    shareNoteAsPdf(context, editorTitle.ifBlank { "Untitled Note" }, editorContent)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                text = { Text("Copy Markdown Code") },
                                onClick = {
                                    showShareMenu = false
                                    copyToClipboard(context, editorContent)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .imePadding()
            ) {
            when (activeViewMode) {
                EditorViewMode.WRITE -> {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tags input aligned closely under top app bar
                    TextField(
                        value = editorTags,
                        onValueChange = { viewModel.updateEditorTags(it) },
                        placeholder = {
                            Text(
                                text = "Tags",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Sell,
                                "Tags",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_tags_input")
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Large clean body writing text area with Live Markdown formatting visual transformation!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        TextField(
                            value = bodyTextFieldValue,
                            onValueChange = { newValue ->
                                val processed = handleListContinuation(bodyTextFieldValue, newValue)
                                if (processed != null) {
                                    bodyTextFieldValue = processed
                                    viewModel.updateEditorContent(processed.text)
                                } else {
                                    bodyTextFieldValue = newValue
                                    if (newValue.text != editorContent) {
                                        viewModel.updateEditorContent(newValue.text)
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    "Start writing here in Markdown...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("editor_body_input"),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp
                            ),
                            visualTransformation = markdownVisualTransformation,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    // Handy Markdown shortcuts helper toolstrip with cursor-aware symbol insertion
                    MarkdownToolstrip(
                        onSymbolSelected = { symbol ->
                            val selection = bodyTextFieldValue.selection
                            val currentText = bodyTextFieldValue.text
                            val newText = StringBuilder(currentText)
                            val insertionIdx = if (selection.start >= 0) selection.start else currentText.length
                            val insertionEndIdx = if (selection.end >= 0) selection.end else currentText.length
                            
                            newText.replace(insertionIdx, insertionEndIdx, symbol)
                            val newCursor = insertionIdx + symbol.length
                            val updatedValue = TextFieldValue(
                                text = newText.toString(),
                                selection = TextRange(newCursor)
                            )
                            bodyTextFieldValue = updatedValue
                            viewModel.updateEditorContent(updatedValue.text)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                EditorViewMode.PREVIEW -> {
                    // Beautiful fluid preview scrollable stack (Standard Obsidian/Notion experience)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = editorTitle.ifBlank { "Untitled Note" },
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (editorTags.isNotBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                editorTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        MarkdownRender(
                            text = editorContent,
                            onContentChanged = { updated -> viewModel.updateEditorContent(updated) }
                        )

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
            } // Closes the outer Column
            
            // Beautiful Floating AI Assist Card Panel
            AnimatedVisibility(
                visible = showAiBottomSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Co-Writer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Zenote AI Co-Writer",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.clearAIStates(); showAiBottomSheet = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isAILoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

                        aiError?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (aiSuggestedTags.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Suggested Tags (Tap to add):",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    aiSuggestedTags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .clickable { viewModel.addTagToEditor(tag) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Presets Grid Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Instant Operations",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .clickable { viewModel.runAIEngine(AIOperation.BEAUTIFY_MARKDOWN) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Brush, "Beautify", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Beautify Note", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                        .clickable { viewModel.runAIEngine(AIOperation.SUMMARIZE) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Summarize, "Summarize", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Summarize Note", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                                        .clickable { viewModel.runAIEngine(AIOperation.EXTRACT_TASKS) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PlaylistAddCheck, "Tasks", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Extract To-Dos", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .clickable { viewModel.runAIEngine(AIOperation.SUGGEST_TAGS) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Sell, "Suggest Tags", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Auto Tagging", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    }
                                }
                            }
                        }

                        // Polish Tone Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Polished Rewrite Tone",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val tones = listOf("Professional", "Casual", "Academic", "Poetic")
                                tones.forEach { tone ->
                                    val isSelected = selectedTone == tone
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable { selectedTone = tone }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tone,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { viewModel.runAIEngine(AIOperation.POLISH_TONE, selectedTone) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-write Tone to $selectedTone")
                            }
                        }

                        // Brainstorm Outline generator
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Brainstorm & Outline Generator",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                            OutlinedTextField(
                                value = customAiPrompt,
                                onValueChange = { customAiPrompt = it },
                                placeholder = { Text("e.g. Brainstorm marketing ideas or design outline...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                singleLine = false,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = { viewModel.runAIEngine(AIOperation.BRAINSTORM_OUTLINE, customAiPrompt) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Outline Template", color = MaterialTheme.colorScheme.onTertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownToolstrip(
    onSymbolSelected: (String) -> Unit
) {
    val items = listOf(
        Pair("H1", "# "),
        Pair("H2", "## "),
        Pair("Bold", "**bold**"),
        Pair("Italic", "*italic*"),
        Pair("Code Block", "\n```\ncode\n```\n"),
        Pair("Blockquote", "\n> "),
        Pair("Bullet", "\n- "),
        Pair("Todo", "\n- [ ] ")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, symbol) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { onSymbolSelected(symbol) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

fun shareMarkdownFile(context: Context, title: String, content: String) {
    try {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val safeTitle = if (title.isBlank()) "Untitled_Note" else title.trim().replace("\\s+".toRegex(), "_")
        val file = File(cacheDir, "$safeTitle.md")
        file.writeText(content)

        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Shared from Zenote AI Workspace: $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Markdown File"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun sharePlainText(context: Context, title: String, content: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "# $title\n\n$content")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Note Content"))
}

fun copyToClipboard(context: Context, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Zenote Content", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied content to clipboard!", Toast.LENGTH_SHORT).show()
    } catch(e: java.lang.Exception) {
        Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareNoteAsPdf(context: Context, title: String, content: String) {
    val activity = context as? android.app.Activity ?: return
    activity.runOnUiThread {
        val webView = android.webkit.WebView(context)
        val htmlContent = convertMarkdownToHtml(title, content)
        
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                try {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                    val jobName = "${if (title.isBlank()) "Note" else title.trim()} Document"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    
                    val printAttributes = android.print.PrintAttributes.Builder()
                        .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(android.print.PrintAttributes.Resolution("id", "print", 300, 300))
                        .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    
                    printManager.print(jobName, printAdapter, printAttributes)
                } catch (e: Exception) {
                    Toast.makeText(context, "Printing system failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }
}

fun convertMarkdownToHtml(title: String, markdown: String): String {
    val lines = markdown.split("\n")
    val htmlBody = StringBuilder()
    
    var inList = false
    var inOrderedList = false
    var inBlockquote = false
    var inCodeBlock = false
    
    for (line in lines) {
        var processedLine = line.trim()
        
        if (processedLine.startsWith("```")) {
            if (inCodeBlock) {
                htmlBody.append("</code></pre>\n")
                inCodeBlock = false
            } else {
                val lang = processedLine.substring(3).trim()
                htmlBody.append("<pre><code class=\"language-$lang\">")
                inCodeBlock = true
            }
            continue
        }
        
        if (inCodeBlock) {
            htmlBody.append(escapeHtml(line)).append("\n")
            continue
        }
        
        if (processedLine.startsWith(">")) {
            if (!inBlockquote) {
                htmlBody.append("<blockquote>")
                inBlockquote = true
            }
            processedLine = processedLine.substring(1).trim()
        } else {
            if (inBlockquote) {
                htmlBody.append("</blockquote>\n")
                inBlockquote = false
            }
        }
        
        val isCheckbox = processedLine.startsWith("- [ ]") || processedLine.startsWith("- [x]") || processedLine.startsWith("- [X]")
        val isUnordered = processedLine.startsWith("- ") || processedLine.startsWith("* ") || processedLine.startsWith("• ")
        val isOrdered = processedLine.matches(Regex("""^\d+\.\s+.*"""))
        
        if (isCheckbox) {
            if (!inList) {
                htmlBody.append("<ul class=\"checklist\">")
                inList = true
                inOrderedList = false
            }
            val checked = processedLine.contains("- [x]", ignoreCase = true)
            val text = processedLine.substring(5).trim()
            val checkboxHtml = if (checked) "☑️ " else "⬜ "
            htmlBody.append("<li><span class=\"checkbox\">$checkboxHtml</span>").append(parseInlineMarkdown(text)).append("</li>\n")
            continue
        } else if (isUnordered) {
            if (!inList || inOrderedList) {
                if (inList) htmlBody.append(if (inOrderedList) "</ol>\n" else "</ul>\n")
                htmlBody.append("<ul>")
                inList = true
                inOrderedList = false
            }
            val text = processedLine.substring(2).trim()
            htmlBody.append("<li>").append(parseInlineMarkdown(text)).append("</li>\n")
            continue
        } else if (isOrdered) {
            if (!inList || !inOrderedList) {
                if (inList) htmlBody.append(if (inOrderedList) "</ol>\n" else "</ul>\n")
                htmlBody.append("<ol>")
                inList = true
                inOrderedList = true
            }
            val match = Regex("""^(\d+)\.\s+(.*)""").find(processedLine)
            val text = match?.groupValues?.get(2) ?: processedLine
            htmlBody.append("<li>").append(parseInlineMarkdown(text)).append("</li>\n")
            continue
        } else {
            if (inList) {
                htmlBody.append(if (inOrderedList) "</ol>\n" else "</ul>\n")
                inList = false
                inOrderedList = false
            }
        }
        
        if (processedLine.startsWith("# ")) {
            htmlBody.append("<h1>").append(parseInlineMarkdown(processedLine.substring(2))).append("</h1>\n")
        } else if (processedLine.startsWith("## ")) {
            htmlBody.append("<h2>").append(parseInlineMarkdown(processedLine.substring(3))).append("</h2>\n")
        } else if (processedLine.startsWith("### ")) {
            htmlBody.append("<h3>").append(parseInlineMarkdown(processedLine.substring(4))).append("</h3>\n")
        } else if (processedLine.startsWith("---") || processedLine.startsWith("***")) {
            htmlBody.append("<hr/>\n")
        } else if (processedLine.isNotEmpty()) {
            htmlBody.append("<p>").append(parseInlineMarkdown(processedLine)).append("</p>\n")
        } else {
            htmlBody.append("<br/>\n")
        }
    }
    
    if (inCodeBlock) htmlBody.append("</code></pre>\n")
    if (inBlockquote) htmlBody.append("</blockquote>\n")
    if (inList) htmlBody.append(if (inOrderedList) "</ol>\n" else "</ul>\n")
    
    val css = """
        body {
            font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            color: #2c3e50;
            line-height: 1.6;
            padding: 30px;
            background-color: #ffffff;
        }
        h1 {
            font-size: 26px;
            color: #111111;
            border-bottom: 2px solid #5f6368;
            padding-bottom: 6px;
            margin-top: 0;
            font-weight: 700;
        }
        h2 {
            font-size: 20px;
            color: #202124;
            margin-top: 20px;
            margin-bottom: 10px;
            font-weight: 600;
        }
        h3 {
            font-size: 16px;
            color: #3c4043;
            margin-top: 16px;
            margin-bottom: 6px;
            font-weight: 600;
        }
        p {
            font-size: 14px;
            margin-bottom: 10px;
        }
        ul, ol {
            padding-left: 20px;
            margin-bottom: 14px;
        }
        li {
            font-size: 14px;
            margin-bottom: 4px;
        }
        .checklist {
            list-style-type: none;
            padding-left: 0;
        }
        .checklist li {
            margin-left: 0;
        }
        blockquote {
            border-left: 4px solid #1a73e8;
            background-color: #f8f9fa;
            padding: 10px 15px;
            margin: 14px 0;
            font-style: italic;
            color: #4a4a4a;
        }
        pre {
            background-color: #f1f3f4;
            border: 1px solid #dadce0;
            border-radius: 6px;
            padding: 12px;
            overflow-x: auto;
            margin: 14px 0;
        }
        code {
            font-family: monospace;
            font-size: 12.5px;
        }
        hr {
            border: 0;
            height: 1px;
            background: #dadce0;
            margin: 20px 0;
        }
    """.trimIndent()
    
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>${escapeHtml(title)}</title>
            <style>
                $css
            </style>
        </head>
        <body>
            <h1>${escapeHtml(title)}</h1>
            $htmlBody
        </body>
        </html>
    """.trimIndent()
}

fun escapeHtml(str: String): String {
    return str.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
}

fun parseInlineMarkdown(text: String): String {
    var result = escapeHtml(text)
    val boldRegex = Regex("""\*\*(.*?)\*\*""")
    result = boldRegex.replace(result) { "<strong>${it.groupValues[1]}</strong>" }
    val italicRegex = Regex("""\*(.*?)\*""")
    result = italicRegex.replace(result) { "<em>${it.groupValues[1]}</em>" }
    val codeRegex = Regex("""`(.*?)`""")
    result = codeRegex.replace(result) { "<code>${it.groupValues[1]}</code>" }
    return result
}
