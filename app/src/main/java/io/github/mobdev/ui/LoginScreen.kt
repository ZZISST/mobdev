package io.github.mobdev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.mobdev.R
import io.github.mobdev.data.AuthStore
import io.github.mobdev.data.Repository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { AuthStore(ctx) }
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(store.username.orEmpty()) }
    var password by remember { mutableStateOf(store.password.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Авто-вход, если креды уже сохранены
    LaunchedEffect(Unit) {
        val savedName = store.username
        val savedPwd = store.password
        if (!savedName.isNullOrBlank() && !savedPwd.isNullOrBlank()) {
            loading = true
            try {
                val token = Repository.login(savedName, savedPwd)
                store.token = token
                onLoggedIn()
            } catch (e: Exception) {
                loading = false
                if (Repository.isUnauthorized(e)) {
                    store.clear()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.login_title))
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.login_username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) return@Button
                loading = true
                scope.launch {
                    try {
                        val token = Repository.login(username.trim(), password)
                        store.username = username.trim()
                        store.password = password
                        store.token = token
                        loading = false
                        onLoggedIn()
                    } catch (e: Exception) {
                        loading = false
                        error = if (Repository.isUnauthorized(e)) {
                            ctx.getString(R.string.login_error)
                        } else {
                            ctx.getString(R.string.login_network_error)
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(stringResource(R.string.login_button))
            }
        }
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(stringResource(R.string.error_dialog_title)) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { error = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}