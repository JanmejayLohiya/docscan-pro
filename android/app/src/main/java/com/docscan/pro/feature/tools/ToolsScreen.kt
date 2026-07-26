package com.docscan.pro.feature.tools

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docscan.pro.util.convertImageFormat
import com.docscan.pro.util.imagesToPdf
import com.docscan.pro.util.pdfToImages
import com.docscan.pro.util.shareFiles
import com.docscan.pro.util.videoToImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onBack: () -> Unit,
    onEditPdf: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Conversion failed", Toast.LENGTH_SHORT).show()
            } finally {
                busy = false
            }
        }
    }

    val imagesToPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) run { shareFiles(context, listOf(imagesToPdf(context, uris)), "application/pdf") }
    }
    val pdfToImagesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run { shareFiles(context, pdfToImages(context, uri), "image/png") }
    }
    val imageConvertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run { shareFiles(context, listOf(convertImageFormat(context, uri)), "image/*") }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run { shareFiles(context, listOf(videoToImage(context, uri)), "image/png") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToolCard("Edit PDF", "Reorder, crop, rotate, erase pages", Color(0xFF4A58C4), Icons.Filled.Edit, onEditPdf)
                ToolCard("Image to PDF", "Combine photos into one PDF", Color(0xFFE5533C), Icons.Filled.PictureAsPdf) {
                    imagesToPdfLauncher.launch(arrayOf("image/*"))
                }
                ToolCard("PDF to image", "Export each page as PNG", Color(0xFF1E9E5A), Icons.Filled.Image) {
                    pdfToImagesLauncher.launch(arrayOf("application/pdf"))
                }
                ToolCard("Convert JPEG / PNG", "Switch an image's format", Color(0xFFE0A020), Icons.Filled.SwapHoriz) {
                    imageConvertLauncher.launch(arrayOf("image/*"))
                }
                ToolCard("Video to image", "Grab a frame from a video", Color(0xFF7C4DFF), Icons.Filled.Videocam) {
                    videoLauncher.launch(arrayOf("video/*"))
                }
            }
            if (busy) {
                Box(
                    Modifier.fillMaxSize().background(Color(0x66000000)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Color.White) }
            }
        }
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}
