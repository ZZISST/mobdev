package io.github.mobdev.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import coil.compose.AsyncImage
import io.github.mobdev.R
import io.github.mobdev.data.AuthStore
import io.github.mobdev.data.BASE_URL
import io.github.mobdev.data.Message
import io.github.mobdev.data.Repository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    channel: String,
    onBack: (() -> Unit)?,
    onImageClick: (String) -> Unit,
    onUnauthorized: () -> Unit
) {
    val ctx = LocalContext.current
    val store = remember { AuthStore(ctx) }
    val scope = rememberCoroutineScope()

    var messages by rememberSaveable(channel) { mutableStateOf<List<Message>>(emptyList()) }
    var loading by rememberSaveable(channel) { mutableStateOf(false) }
    var input by rememberSaveable(channel) { mutableStateOf("") }
    var canLoadMore by rememberSaveable(channel) { mutableStateOf(true) }
    val listState = rememberLazyListState()

    suspend fun loadInitial() {
        loading = true
        try {
            val loaded = Repository.messages(channel, lastKnownId = 0, reverse = false)
            messages = loaded
            canLoadMore = loaded.size >= 20
        } catch (e: Exception) {
            if (Repository.isUnauthorized(e)) {
                store.token = null
                onUnauthorized()
            }
        } finally {
            loading = false
        }
    }

    suspend fun loadMore() {
        if (loading || !canLoadMore || messages.isEmpty()) return
        loading = true
        try {
            val newestId = messages.maxOf { it.id ?: 0L }
            val newer = Repository.messages(channel, lastKnownId = newestId, reverse = false)
            if (newer.isEmpty()) {
                canLoadMore = false
            } else {
                messages = (messages + newer).distinctBy { it.id }
                if (newer.size < 20) canLoadMore = false
            }
        } catch (e: Exception) {
            if (Repository.isUnauthorized(e)) {
                store.token = null
                onUnauthorized()
            }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(channel) {
        if (messages.isEmpty()) {
            loadInitial()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(channel) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }
            )
        }
    ) { pvs ->
        Column(Modifier.padding(pvs).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty() && loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (messages.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_messages))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages, key = { it.id ?: (it.from + it.time) }) { msg ->
                            MessageRow(msg, onImageClick)
                            HorizontalDivider()
                        }
                        if (canLoadMore) {
                            item {
                                Button(
                                    onClick = { scope.launch { loadMore() } },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    enabled = !loading
                                ) {
                                    Text(stringResource(R.string.load_more))
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.message_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = input.trim()
                        if (text.isEmpty()) return@Button
                        val token = store.token ?: return@Button
                        val name = store.username ?: return@Button
                        scope.launch {
                            try {
                                Repository.sendText(token, name, channel, text)
                                input = ""
                                // грузим все остальные сообщения
                                var keepLoading = true
                                while (keepLoading) {
                                    val newestId = messages.maxOfOrNull { it.id ?: 0L } ?: 0L
                                    val newer = Repository.messages(channel, lastKnownId = newestId, reverse = false)
                                    if (newer.isEmpty()) {
                                        keepLoading = false
                                    } else {
                                        messages = (messages + newer).distinctBy { it.id }
                                        if (newer.size < 20) keepLoading = false
                                    }
                                }
                                canLoadMore = false
                                // летим вниз к последним сообщениями
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            } catch (e: Exception) {
                                if (Repository.isUnauthorized(e)) {
                                    store.token = null
                                    onUnauthorized()
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.send))
                }
            }
        }
    }
}

@Composable
private fun MessageRow(msg: Message, onImageClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = msg.from,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(4.dp))
        when {
            msg.data.text != null -> {
                Text(msg.data.text.text)
            }
            msg.data.image?.link != null -> {
                val link = msg.data.image.link
                AsyncImage(
                    model = "${BASE_URL}thumb/$link",
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .clickable { onImageClick(link) }
                )
            }
        }
    }
}