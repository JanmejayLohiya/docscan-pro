package com.docscan.pro.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.docscan.pro.domain.Page
import com.docscan.pro.feature.scan.rememberScanLauncher
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addPages = rememberScanLauncher(onScanned = viewModel::addPages)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.document?.name ?: "Edit",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "${state.pages.size} pages",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(state.pages, key = { _, p -> p.id }) { index, page ->
                    PageRow(
                        index = index,
                        page = page,
                        isFirst = index == 0,
                        isLast = index == state.pages.lastIndex,
                        onUp = { viewModel.reorder(index, index - 1) },
                        onDown = { viewModel.reorder(index, index + 1) },
                        onRotate = { viewModel.rotate(page.id) },
                        onRemove = { viewModel.remove(page.id) },
                    )
                    HorizontalDivider()
                }
            }
            Button(
                onClick = addPages,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Add pages") }
        }
    }
}

@Composable
private fun PageRow(
    index: Int,
    page: Page,
    isFirst: Boolean,
    isLast: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = File(page.imagePath),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 60.dp, height = 78.dp),
        )
        Text("Page ${index + 1}", modifier = Modifier.weight(1f))
        // Reorder / rotate / remove controls (drag-reorder is a slice-B polish).
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onUp, enabled = !isFirst) { Text("↑") }
                OutlinedButton(onClick = onDown, enabled = !isLast) { Text("↓") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onRotate) { Text("Rotate") }
                OutlinedButton(onClick = onRemove) { Text("Delete") }
            }
        }
    }
}
