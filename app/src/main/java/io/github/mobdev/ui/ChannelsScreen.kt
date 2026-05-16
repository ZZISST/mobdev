package io.github.mobdev.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mobdev.R
import io.github.mobdev.data.AuthStore
import io.github.mobdev.data.Repository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    selectedChannel: String? = null,
    onChannelClick: (String) -> Unit,
    onLogout: () -> Unit,
    onUnauthorized: () -> Unit
) {
    val ctx = LocalContext.current
    val store = remember { AuthStore(ctx) }
    val scope = rememberCoroutineScope()

    var channels by rememberSaveable { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(Unit) {
        if (channels == null) {
            try {
                channels = Repository.channels()
            } catch (e: Exception) {
                if (Repository.isUnauthorized(e)) {
                    store.token = null
                    onUnauthorized()
                } else {
                    channels = emptyList()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.channels_title)) },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            val t = store.token
                            if (t != null) Repository.logout(t)
                            store.clear()
                            onLogout()
                        }
                    }) {
                        Text(stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { pvs ->
        Box(Modifier.padding(pvs).fillMaxSize()) {
            val list = channels
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_messages))
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(list) { channel ->
                        val isSelected = channel == selectedChannel
                        Text(
                            text = channel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { onChannelClick(channel) }
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}