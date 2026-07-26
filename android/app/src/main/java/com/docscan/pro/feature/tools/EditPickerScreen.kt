package com.docscan.pro.feature.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docscan.pro.feature.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPickerScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit a PDF") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        if (state.documents.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No documents to edit yet.", Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(state.documents, key = { it.id }) { doc ->
                    ListItem(
                        modifier = Modifier.clickable { onEdit(doc.id) },
                        headlineContent = { Text(doc.name) },
                        supportingContent = { Text("${doc.pageCount} pages · ${doc.format}") },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
