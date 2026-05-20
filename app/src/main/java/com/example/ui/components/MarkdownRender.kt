package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.material.icons.outlined.ContentCopy

@Composable
fun MarkdownRender(
    text: String,
    onContentChanged: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var isInCodeBlock = false
        val currentCodeBlock = StringBuilder()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            // 1. Code block handling (```)
            if (line.trim().startsWith("```")) {
                if (isInCodeBlock) {
                    // Output the finished block
                    CodeBlockLayout(code = currentCodeBlock.toString().trimEnd())
                    currentCodeBlock.clear()
                    isInCodeBlock = false
                } else {
                    isInCodeBlock = true
                }
                i++
                continue
            }

            if (isInCodeBlock) {
                currentCodeBlock.append(line).append("\n")
                i++
                continue
            }

            val trimmedLine = line.trim()

            when {
                // 2. Headings
                trimmedLine.startsWith("# ") -> {
                    val headingText = trimmedLine.substring(2)
                    Text(
                        text = parseMarkdownToAnnotatedString(headingText, primaryColor),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                trimmedLine.startsWith("## ") -> {
                    val headingText = trimmedLine.substring(3)
                    Text(
                        text = parseMarkdownToAnnotatedString(headingText, primaryColor),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = labelColor
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmedLine.startsWith("### ") -> {
                    val headingText = trimmedLine.substring(4)
                    Text(
                        text = parseMarkdownToAnnotatedString(headingText, primaryColor),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // 3. Horizontal Rule
                trimmedLine == "---" -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // 4. Blockquotes
                trimmedLine.startsWith("> ") -> {
                    val quoteText = trimmedLine.substring(2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(primaryColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = parseMarkdownToAnnotatedString(quoteText, primaryColor),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                color = onSurfaceColor.copy(alpha = 0.85f)
                            ),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }

                // 5. Checklist task items (- [ ] / - [x])
                (trimmedLine.startsWith("- [ ] ") || trimmedLine.startsWith("- [x] ")) -> {
                    val isChecked = trimmedLine.startsWith("- [x] ")
                    val taskContent = if (isChecked) trimmedLine.substring(6) else trimmedLine.substring(6)
                    val lineIndex = i // Capture local line index for task updates

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = if (isChecked) "Checked" else "Unchecked",
                            tint = if (isChecked) primaryColor else onSurfaceColor.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = onContentChanged != null) {
                                    onContentChanged?.let { update ->
                                        val newLines = lines.toMutableList()
                                        val toggledLine = if (isChecked) {
                                            line.replaceFirst("- [x] ", "- [ ] ")
                                        } else {
                                            line.replaceFirst("- [ ] ", "- [x] ")
                                        }
                                        newLines[lineIndex] = toggledLine
                                        update(newLines.joinToString("\n"))
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseMarkdownToAnnotatedString(taskContent, primaryColor),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isChecked) onSurfaceColor.copy(alpha = 0.5f) else onSurfaceColor
                            )
                        )
                    }
                }

                // 6. Styled Bullet Lists
                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") -> {
                    val bulletContent = trimmedLine.substring(2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseMarkdownToAnnotatedString(bulletContent, primaryColor),
                            style = MaterialTheme.typography.bodyLarge.copy(color = onSurfaceColor)
                        )
                    }
                }

                // 7. Standard lines of text (with potential trailing space to preserve lines)
                else -> {
                    if (line.isNotEmpty()) {
                        Text(
                            text = parseMarkdownToAnnotatedString(line, primaryColor),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = onSurfaceColor,
                                lineHeight = 22.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
            i++
        }

        // Catch unclosed trailing code blocks
        if (isInCodeBlock && currentCodeBlock.isNotEmpty()) {
            CodeBlockLayout(code = currentCodeBlock.toString().trimEnd())
        }
    }
}

@Composable
fun CodeBlockLayout(code: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RAW CODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                IconButton(
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Markdown Code Block", code)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Copy failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.ContentCopy,
                        contentDescription = "Copy code block",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = code,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

fun parseMarkdownToAnnotatedString(text: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        while (i < len) {
            // 1. Double character bold "**" or "__"
            if (i + 1 < len && ((text[i] == '*' && text[i+1] == '*') || (text[i] == '_' && text[i+1] == '_'))) {
                val mark = text.substring(i, i + 2)
                val closing = text.indexOf(mark, i + 2)
                if (closing != -1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(parseMarkdownToAnnotatedString(text.substring(i + 2, closing), primaryColor))
                    }
                    i = closing + 2
                    continue
                }
            }

            // 2. Double character strikethrough "~~"
            if (i + 1 < len && text[i] == '~' && text[i+1] == '~') {
                val closing = text.indexOf("~~", i + 2)
                if (closing != -1) {
                    withStyle(style = SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                        append(parseMarkdownToAnnotatedString(text.substring(i + 2, closing), primaryColor))
                    }
                    i = closing + 2
                    continue
                }
            }

            // 3. Single character italic "*" or "_"
            if (text[i] == '*' || text[i] == '_') {
                val mark = text[i].toString()
                val closing = text.indexOf(mark, i + 1)
                if (closing != -1) {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(parseMarkdownToAnnotatedString(text.substring(i + 1, closing), primaryColor))
                    }
                    i = closing + 1
                    continue
                }
            }

            // 4. Single character inline code "`"
            if (text[i] == '`') {
                val closing = text.indexOf('`', i + 1)
                if (closing != -1) {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = primaryColor.copy(alpha = 0.08f),
                            color = primaryColor,
                            fontSize = 13.sp
                        )
                    ) {
                        append(text.substring(i + 1, closing))
                    }
                    i = closing + 1
                    continue
                }
            }

            // Standard character append
            append(text[i].toString())
            i++
        }
    }
}
