package com.docscan.pro.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docscan.pro.domain.Document
import com.docscan.pro.feature.scan.rememberScanLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDocument: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val launchScan = rememberScanLauncher(onScanned = viewModel::onScanned)

    Scaffold(
        topBar = { TopAppBar(title = { Text("DocScan Pro") }) },
        bottomBar = {
            HomeBottomBar(
                selected = tab,
                onHome = { tab = 0 },
                onScan = launchScan,
                onAccount = { tab = 1 },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeContent(state, onOpenDocument, viewModel::rename, viewModel::delete)
                else -> AccountTab()
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recent", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            SyncPill(allSynced = state.documents.isNotEmpty() && state.documents.all { it.syncState == "SYNCED" })
        }

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.documents.isEmpty() ->
                ComingSoon(Icons.Filled.DocumentScanner, "No documents yet", "Tap Scan to capture your first document.")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                // Recent activity = the 3 most recently updated PDFs.
                items(state.documents.take(3), key = { it.id }) { doc ->
                    DocumentRow(doc, onOpen, onRename, onDelete)
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentRow(
    doc: Document,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable { onOpen(doc.id) },
        leadingContent = {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PictureAsPdf, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
        headlineContent = { Text(doc.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("${doc.pageCount} pages · ${doc.format} · ${formatSize(doc.sizeBytes)}") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SyncIcon(doc.syncState)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = { showRename = true; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = { onDelete(doc.id); menuOpen = false },
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )

    if (showRename) {
        RenameDialog(
            current = doc.name,
            onDismiss = { showRename = false },
            onConfirm = { name -> onRename(doc.id, name); showRename = false },
        )
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename document") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HomeBottomBar(
    selected: Int,
    onHome: () -> Unit,
    onScan: () -> Unit,
    onAccount: () -> Unit,
) {
    Column {
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).height(76.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTab(Modifier.weight(1f), Icons.Filled.Home, "Home", selected == 0, onHome)
            Column(
                Modifier.weight(1f).clickable(onClick = onScan),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.DocumentScanner,
                        "Scan",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text("Scan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            NavTab(Modifier.weight(1f), Icons.Filled.Person, "Account", selected == 1, onAccount)
        }
    }
}

@Composable
private fun NavTab(modifier: Modifier, icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun SyncIcon(syncState: String) {
    when (syncState) {
        "SYNCED" -> Icon(Icons.Filled.CheckCircle, "Synced", tint = MaterialTheme.colorScheme.primary)
        "PENDING", "UPLOADING" -> Icon(Icons.Filled.Sync, "Syncing", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Icon(Icons.Filled.CloudOff, "On this device", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SyncPill(allSynced: Boolean) {
    val (label, container, content) = if (allSynced) {
        Triple("All synced", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    } else {
        Triple("On this device", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun ComingSoon(icon: ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTab() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column {
                Text("Guest", style = MaterialTheme.typography.titleMedium)
                Text("Not signed in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text("Free plan") },
            supportingContent = { Text("Upgrade to Pro in a later update") },
            leadingContent = { Icon(Icons.Filled.CheckCircle, null) },
        )
        ListItem(
            headlineContent = { Text("Cloud backup") },
            supportingContent = { Text("Connect Google Drive — coming soon") },
            leadingContent = { Icon(Icons.Filled.CloudOff, null) },
        )
        ListItem(
            headlineContent = { Text("Settings") },
            leadingContent = { Icon(Icons.Filled.Settings, null) },
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "${bytes / 1_000} KB"
    else -> "$bytes B"
}
