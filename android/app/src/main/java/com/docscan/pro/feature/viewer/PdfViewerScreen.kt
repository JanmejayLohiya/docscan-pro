package com.docscan.pro.feature.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docscan.pro.util.shareFiles
import java.io.File

private enum class ViewerTheme(val label: String, val background: Color, val filter: ColorFilter?) {
    Light("Light", Color(0xFFECECEC), null),
    Dark(
        "Dark",
        Color(0xFF111318),
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        ),
    ),
    Sepia(
        "Sepia",
        Color(0xFFEFE6D2),
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        ),
    ),
    DocScan("DocScan", Color(0xFFE3E6FA), null),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    onBack: () -> Unit,
    viewModel: PdfViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var theme by remember { mutableStateOf(ViewerTheme.Light) }
    var themeMenu by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "Document" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    IconButton(onClick = { searching = !searching }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search in document")
                    }
                    Box {
                        IconButton(onClick = { themeMenu = true }) {
                            Icon(Icons.Filled.Palette, contentDescription = "Reading theme")
                        }
                        DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                            ViewerTheme.entries.forEach { t ->
                                DropdownMenuItem(text = { Text(t.label) }, onClick = { theme = t; themeMenu = false })
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            if (state.filePath.isNotBlank()) {
                                shareFiles(context, listOf(File(state.filePath)), "application/pdf")
                            }
                        },
                    ) { Icon(Icons.Filled.Share, contentDescription = "Share") }
                },
            )
        },
    ) { padding ->
        val showResults = searching && query.isNotBlank()
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(padding)) {
            if (searching) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Search this document") },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
                HorizontalDivider()
            }
            val bg = if (showResults) MaterialTheme.colorScheme.surface else theme.background
            Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CircularProgressIndicator()
                    showResults -> {
                        val matches = remember(query, state.ocrText) { searchSnippets(state.ocrText, query) }
                        if (matches.isEmpty()) {
                            Text("No matches in this document.", Modifier.padding(24.dp))
                        } else {
                            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                item {
                                    Text(
                                        "${matches.size} match${if (matches.size == 1) "" else "es"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                    )
                                }
                                items(matches) { snippet ->
                                    Text(snippet, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 10.dp))
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                    state.pages.isEmpty() -> Text("Couldn't open this document.", Modifier.padding(24.dp))
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.pages) { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                colorFilter = theme.filter,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Returns context snippets around each occurrence of [query] in [text] (case-insensitive). */
private fun searchSnippets(text: String, query: String): List<String> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    val out = mutableListOf<String>()
    var i = text.indexOf(q, 0, ignoreCase = true)
    while (i >= 0 && out.size < 100) {
        val start = (i - 30).coerceAtLeast(0)
        val end = (i + q.length + 30).coerceAtMost(text.length)
        out.add("…" + text.substring(start, end).replace('\n', ' ').trim() + "…")
        i = text.indexOf(q, i + q.length, ignoreCase = true)
    }
    return out
}
