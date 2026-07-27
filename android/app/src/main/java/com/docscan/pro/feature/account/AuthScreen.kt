package com.docscan.pro.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onDone: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    var signUp by remember { mutableStateOf(false) }
    var usePhone by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }

    val canContinue = if (usePhone) phone.isNotBlank() else email.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (signUp) "Create account" else "Log in") },
                navigationIcon = { TextButton(onClick = onDone) { Text("Close") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !signUp,
                    onClick = { signUp = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Log in") }
                SegmentedButton(
                    selected = signUp,
                    onClick = { signUp = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Sign up") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!usePhone, { usePhone = false }, { Text("Email") })
                FilterChip(usePhone, { usePhone = true }, { Text("Phone") })
            }

            if (usePhone) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    singleLine = true,
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!codeSent) {
                    OutlinedButton(onClick = { codeSent = true }, enabled = phone.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text("Send code")
                    }
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        singleLine = true,
                        label = { Text("Verification code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = { viewModel.signIn(email, phone); onDone() },
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (signUp) "Create account" else "Log in") }

            Spacer(Modifier.height(4.dp))
            Text(
                "Secure phone/email verification is added when cloud sync (Firebase) is connected.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
