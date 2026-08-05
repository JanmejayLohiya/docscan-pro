package com.docscan.pro.feature.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docscan.pro.util.PageOcr
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
    var translateMenu by remember { mutableStateOf(false) }

    val tokens = remember(query, searching) {
        if (searching) query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() } else emptyList()
    }
    val translated = state.translatedPages
    val matchCount = remember(tokens, state.pageOcr) {
        if (tokens.isEmpty()) 0
        else state.pageOcr.sumOf { page -> page.words.count { w -> tokens.any { w.text.lowercase().contains(it) } } }
    }

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
                    Box {
                        IconButton(onClick = { translateMenu = true }) {
                            Icon(Icons.Filled.Translate, contentDescription = "Translate")
                        }
                        DropdownMenu(expanded = translateMenu, onDismissRequest = { translateMenu = false }) {
                            TRANSLATE_TARGETS.forEach { (label, tag) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { translateMenu = false; viewModel.translate(tag, label) },
                                )
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (searching) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Search this document") },
                    supportingText = if (query.isNotBlank()) {
                        { Text("$matchCount match${if (matchCount == 1) "" else "es"} highlighted") }
                    } else null,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
                HorizontalDivider()
            }
            if (translated != null) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Translated to ${state.translatedLang}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.clearTranslation() }) { Text("Show original") }
                }
                HorizontalDivider()
            }

            Box(Modifier.fillMaxSize().background(theme.background), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CircularProgressIndicator()
                    state.translating -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Translating to ${state.translatedLang}…", Modifier.padding(horizontal = 24.dp))
                    }
                    state.translateError != null && translated == null -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            state.translateError!!,
                            Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = { viewModel.clearTranslation() }) { Text("Dismiss") }
                    }
                    state.pages.isEmpty() -> Text("Couldn't open this document.", Modifier.padding(24.dp))
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(state.pages) { index, bmp ->
                            PageView(
                                bmp = bmp,
                                ocr = state.pageOcr.getOrNull(index),
                                translatedBlocks = translated?.getOrNull(index),
                                highlightTokens = tokens,
                                colorFilter = theme.filter,
                                pageNumber = index + 1,
                                pageCount = state.pages.size,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders one page image and, on top of it, optional overlays in the page's own
 * pixel space: yellow highlight boxes for search matches, and translated text
 * boxes (masking the original) for the in-layout translation view.
 */
@Composable
private fun PageView(
    bmp: Bitmap,
    ocr: PageOcr?,
    translatedBlocks: List<com.docscan.pro.feature.viewer.TranslatedBlock>?,
    highlightTokens: List<String>,
    colorFilter: ColorFilter?,
    pageNumber: Int,
    pageCount: Int,
) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth.toFloat()
        // OCR was run on this exact bitmap, so its coordinate space matches bmp.
        val scale = widthPx / bmp.width
        val displayedHeightPx = bmp.height * scale
        Box(Modifier.fillMaxWidth().height(with(density) { displayedHeightPx.toDp() })) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page $pageNumber of $pageCount",
                contentScale = ContentScale.FillWidth,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize(),
            )
            // Search highlights.
            if (ocr != null && highlightTokens.isNotEmpty()) {
                ocr.words.forEach { w ->
                    if (highlightTokens.any { w.text.lowercase().contains(it) }) {
                        Box(
                            Modifier
                                .offset(
                                    x = with(density) { (w.left * scale).toDp() },
                                    y = with(density) { (w.top * scale).toDp() },
                                )
                                .size(
                                    width = with(density) { ((w.right - w.left) * scale).toDp() },
                                    height = with(density) { ((w.bottom - w.top) * scale).toDp() },
                                )
                                .background(Color(0x88FFEB3B)),
                        )
                    }
                }
            }
            // Translation overlay (mask original block, draw translated text in place).
            translatedBlocks?.forEach { b ->
                val boxH = (b.bottom - b.top) * scale
                val fontSp = with(density) { (boxH * 0.32f).toDp().value }.coerceIn(8f, 15f)
                Box(
                    Modifier
                        .offset(
                            x = with(density) { (b.left * scale).toDp() },
                            y = with(density) { (b.top * scale).toDp() },
                        )
                        .size(
                            width = with(density) { ((b.right - b.left) * scale).toDp() },
                            height = with(density) { (boxH).toDp() },
                        )
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Text(
                        b.text,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = fontSp.sp,
                        lineHeight = (fontSp * 1.1f).sp,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

/** Target languages offered for on-device translation (label to BCP-47 tag). */
private val TRANSLATE_TARGETS = listOf(
    "English" to "en",
    "Hindi" to "hi",
    "Spanish" to "es",
    "French" to "fr",
    "German" to "de",
    "Chinese" to "zh",
    "Arabic" to "ar",
    "Russian" to "ru",
    "Portuguese" to "pt",
    "Japanese" to "ja",
)
