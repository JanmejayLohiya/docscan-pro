package com.docscan.pro.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
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
    val context = LocalContext.current
    val addPages = rememberScanLauncher(onScanned = viewModel::addPages)

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) result.uriContent?.let(viewModel::applyCrop)
    }
    val insertLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.insertImages(uris) }

    fun startCrop(page: Page) {
        viewModel.beginCrop(page.id)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(page.imagePath),
        )
        cropLauncher.launch(CropImageContractOptions(uri, CropImageOptions()))
    }

    var erasingPage by remember { mutableStateOf<Page?>(null) }
    val erasing = erasingPage
    if (erasing != null) {
        EraseOverlay(
            imagePath = erasing.imagePath,
            onCancel = { erasingPage = null },
            onApply = { strokes, w, h, brush ->
                viewModel.erase(erasing.id, strokes, w, h, brush)
                erasingPage = null
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.document?.name ?: "Edit", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        onCrop = { startCrop(page) },
                        onRotate = { viewModel.rotate(page.id) },
                        onResize = { viewModel.resize(page.id) },
                        onErase = { erasingPage = page },
                        onRemove = { viewModel.remove(page.id) },
                    )
                    HorizontalDivider()
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { insertLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
                    Text("Insert image")
                }
                OutlinedButton(onClick = addPages, modifier = Modifier.weight(1f)) {
                    Text("Add pages")
                }
            }
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
    onCrop: () -> Unit,
    onRotate: () -> Unit,
    onResize: () -> Unit,
    onErase: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = File(page.imagePath),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 60.dp, height = 78.dp),
            )
            Text("Page ${index + 1}", style = MaterialTheme.typography.titleMedium)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = onUp, enabled = !isFirst) { Text("↑") }
            OutlinedButton(onClick = onDown, enabled = !isLast) { Text("↓") }
            OutlinedButton(onClick = onCrop) { Text("Crop") }
            OutlinedButton(onClick = onRotate) { Text("Rotate") }
            OutlinedButton(onClick = onResize) { Text("Resize") }
            OutlinedButton(onClick = onErase) { Text("Erase") }
            OutlinedButton(onClick = onRemove) { Text("Delete") }
        }
    }
}
