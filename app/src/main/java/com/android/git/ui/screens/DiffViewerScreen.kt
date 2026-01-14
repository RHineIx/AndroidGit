package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.data.GitManager
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffViewerScreen(
    gitManager: GitManager,
    filePath: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var diffContent by remember { mutableStateOf("Loading diff...") }
    var isLoading by remember { mutableStateOf(true) }

    // Determine file extension for syntax highlighting
    val extension = remember(filePath) {
        filePath.substringAfterLast('.', "").lowercase(Locale.ROOT)
    }

    BackHandler { onBack() }

    LaunchedEffect(filePath) {
        scope.launch {
            isLoading = true
            diffContent = gitManager.getFileDiff(filePath)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Diff Viewer")
                        Text(
                            text = filePath,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .background(Color(0xFF1E1E1E)) // Editor Dark Background
                    ) {
                        if (diffContent.isBlank()) {
                            Text(
                                "No changes or binary file.",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            diffContent.lines().forEach { line ->
                                DiffLine(line, extension)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiffLine(line: String, extension: String) {
    // 1. Determine Background Color based on Diff char (+/-)
    val bgColor = when {
        line.startsWith("+") && !line.startsWith("+++") -> Color(0xFF1B5E20) // Green Added
        line.startsWith("-") && !line.startsWith("---") -> Color(0xFF4A1919) // Red Deleted
        line.startsWith("@@") -> Color(0xFF2D2D2D) // Chunk Header
        else -> Color.Transparent
    }

    // 2. Syntax Highlighting Logic
    val styledText = remember(line, extension) {
        buildAnnotatedString {
            // A. Diff Header Formatting
            if (line.startsWith("+++") || line.startsWith("---")) {
                withStyle(SpanStyle(color = Color.Gray)) { append(line) }
                return@buildAnnotatedString
            }
            if (line.startsWith("@@")) {
                withStyle(SpanStyle(color = Color(0xFFBB86FC))) { append(line) }
                return@buildAnnotatedString
            }

            // B. Code Formatting
            val codeColor = Color(0xFFD4D4D4) // Default code color
            
            // Apply highlighting based on regex
            val keywords = SyntaxHighlighter.getKeywords(extension)
            val comments = SyntaxHighlighter.getCommentRegex(extension)
            val strings = Regex("\"(.*?)\"|'(.*?)'")

            // We need to parse strictly, but for performance in a list, we do a simple multi-pass.
            // Note: This is a basic highlighter. A full lexer is too heavy for a simple view.
            
            var lastIndex = 0
            val text = line

            // Simple tokenization by splitting on non-alphanumeric (keeping delimiters)
            // This is a naive approach for speed.
            val tokens = text.splitWithDelimiter(Regex("[^a-zA-Z0-9_]"))
            
            tokens.forEach { token ->
                when {
                    keywords.contains(token) -> {
                        withStyle(SpanStyle(color = Color(0xFFCC7832), fontWeight = FontWeight.Bold)) { // Orange Keyword
                            append(token)
                        }
                    }
                    token.matches(Regex("[0-9]+")) -> {
                        withStyle(SpanStyle(color = Color(0xFF6897BB))) { // Blue Number
                            append(token)
                        }
                    }
                    // Basic String detection (very naive, works for single line strings)
                    token.startsWith("\"") || token.startsWith("'") -> {
                         withStyle(SpanStyle(color = Color(0xFF6A8759))) { // Green String
                            append(token)
                        }
                    }
                    // Basic Comment detection (whole line or end of line)
                    token.startsWith("//") || token.startsWith("#") -> {
                         withStyle(SpanStyle(color = Color(0xFF808080))) { // Grey Comment
                            append(token)
                        }
                    }
                    else -> {
                        // Regular text or symbols
                        val symbolColor = if(token.isBlank()) codeColor else if (token.matches(Regex("[(){}\\[\\].,;]"))) Color(0xFFFFC66D) else codeColor
                        withStyle(SpanStyle(color = symbolColor)) {
                            append(token)
                        }
                    }
                }
            }
        }
    }

    // 3. Render
    Text(
        text = styledText,
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

// --- Internal Helper for Syntax Highlighting ---
object SyntaxHighlighter {
    fun getKeywords(extension: String): Set<String> {
        return when (extension) {
            "kt", "kts", "java", "scala" -> setOf(
                "package", "import", "class", "interface", "object", "val", "var", "fun",
                "return", "if", "else", "for", "while", "do", "when", "try", "catch", "throw",
                "true", "false", "null", "this", "super", "private", "protected", "public",
                "internal", "override", "companion", "lateinit", "const", "suspend", "data", "enum"
            )
            "py" -> setOf(
                "def", "class", "import", "from", "return", "if", "elif", "else", "for", "while",
                "try", "except", "finally", "raise", "True", "False", "None", "and", "or", "not",
                "in", "is", "lambda", "with", "as", "pass", "break", "continue", "global"
            )
            "xml", "html" -> setOf(
                "android", "xmlns", "tools", "app", "id", "layout", "width", "height",
                "text", "color", "style", "name"
            )
            "js", "ts", "json" -> setOf(
                "function", "var", "let", "const", "return", "if", "else", "for", "while",
                "class", "extends", "import", "export", "default", "true", "false", "null", "undefined"
            )
            else -> emptySet()
        }
    }

    fun getCommentRegex(extension: String): Regex {
        return when (extension) {
            "py", "sh", "yaml", "yml", "toml", "properties" -> Regex("#.*")
            else -> Regex("//.*")
        }
    }
}

// Extension to split but keep delimiters for coloring
private fun String.splitWithDelimiter(regex: Regex): List<String> {
    val result = mutableListOf<String>()
    var lastMatchEnd = 0
    regex.findAll(this).forEach { match ->
        if (match.range.first > lastMatchEnd) {
            result.add(this.substring(lastMatchEnd, match.range.first))
        }
        result.add(match.value)
        lastMatchEnd = match.range.last + 1
    }
    if (lastMatchEnd < this.length) {
        result.add(this.substring(lastMatchEnd))
    }
    return result
}