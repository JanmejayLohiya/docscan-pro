package com.docscan.pro.feature.account

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                Column(Modifier.weight(1f)) {
                    Text(
                        state.name.ifBlank { if (state.signedIn) "Signed in" else "Guest" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    val subtitle = when {
                        state.identifier.isNotBlank() -> state.identifier
                        state.signedIn -> "Signed in"
                        else -> "Not signed in"
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit profile") }
            }

            if (state.signedIn) {
                OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text("Sign out")
                }
            } else {
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text("Sign in or create account")
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Text(
                "Settings",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            ListItem(
                leadingContent = { Icon(Icons.Filled.CloudOff, null) },
                headlineContent = { Text("Cloud backup") },
                supportingContent = { Text("Sync scans across devices — coming soon") },
            )
            ListItem(
                leadingContent = { Icon(Icons.Filled.DarkMode, null) },
                headlineContent = { Text("Appearance") },
                supportingContent = { Text("Follows your system theme") },
            )
            ListItem(
                leadingContent = { Icon(Icons.Filled.Info, null) },
                headlineContent = { Text("About") },
                supportingContent = { Text("DocScan Pro v0.1.0") },
            )
        }
    }

    if (editing) {
        EditProfileDialog(
            initialName = state.name,
            initialEmail = state.email,
            onDismiss = { editing = false },
            onSave = { name, email -> viewModel.updateInfo(name, email); editing = false },
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, singleLine = true, label = { Text("Email") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, email) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
