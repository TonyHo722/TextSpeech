package com.example.textspeech

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    data class SharedUrl(val url: String, val timestamp: Long = System.currentTimeMillis())
    private val sharedUrlState = mutableStateOf(SharedUrl(""))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setIntent(intent)
        handleIntent(intent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(sharedUrl = sharedUrlState.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val urlRegex = Regex("(https?://[^\\s]+)")
            val match = urlRegex.find(sharedText)
            sharedUrlState.value = SharedUrl(match?.value ?: sharedText)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(sharedUrl: SharedUrl) {
        val coroutineScope = rememberCoroutineScope()
        var extractedText by remember { mutableStateOf("") }
        var chunks by remember { mutableStateOf(listOf<String>()) }
        var isLoading by remember { mutableStateOf(false) }
        var isPlaying by remember { mutableStateOf(false) }
        var playbackSpeed by remember { mutableStateOf(1.0f) }
        var currentPlayingIndex by remember { mutableIntStateOf(-1) }

        // Menu & Dialog state
        var showMenu by remember { mutableStateOf(false) }
        var showUrlDialog by remember { mutableStateOf(false) }
        var dialogUrlInput by remember { mutableStateOf("") }

        val listState = rememberLazyListState()

        fun sendPlaybackAction(action: String, extra: Intent.() -> Unit = {}) {
            val intent = Intent(this@MainActivity, TtsPlaybackService::class.java).apply {
                this.action = action
                extra()
            }
            startService(intent)
        }

        // Auto-extract and Auto-play when sharedUrl changes
        LaunchedEffect(sharedUrl) {
            val url = sharedUrl.url
            if (url.isNotEmpty()) {
                isLoading = true
                extractedText = ArticleExtractor.extractTextFromUrl(url)
                chunks = extractedText.split("\n").filter { it.trim().isNotEmpty() }
                isLoading = false

                // Trigger playback automatically after extraction
                if (extractedText.isNotEmpty()) {
                    sendPlaybackAction(TtsPlaybackService.ACTION_PLAY) {
                        putExtra(TtsPlaybackService.EXTRA_TEXT, extractedText)
                    }
                }
            }
        }

        // Register BroadcastReceiver for playback index updates
        DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == TtsPlaybackService.BROADCAST_INDEX) {
                        currentPlayingIndex = intent.getIntExtra(TtsPlaybackService.EXTRA_CURRENT_INDEX, -1)
                        isPlaying = intent.getBooleanExtra(TtsPlaybackService.EXTRA_IS_PLAYING, false)
                    }
                }
            }
            val filter = IntentFilter(TtsPlaybackService.BROADCAST_INDEX)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
            onDispose { unregisterReceiver(receiver) }
        }

        // Auto-scroll to the currently playing sentence
        LaunchedEffect(currentPlayingIndex) {
            if (currentPlayingIndex >= 0 && currentPlayingIndex < chunks.size) {
                listState.animateScrollToItem(currentPlayingIndex)
            }
        }


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TTS Reader") },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Input URL") },
                                onClick = {
                                    showMenu = false
                                    dialogUrlInput = ""
                                    showUrlDialog = true
                                }
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp)
            ) {
                // ── Article Text with Sentence Highlighting ────────────
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (chunks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Open the menu ⋮ to input a URL,\nor share a webpage from your browser.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(chunks) { index, chunk ->
                                val isHighlighted = index == currentPlayingIndex
                                val backgroundColor = if (isHighlighted) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }

                                Text(
                                    text = chunk,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(backgroundColor)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .pointerInput(index) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    // Seek to this sentence and start playing
                                                    isPlaying = true // Optimistic UI update
                                                    sendPlaybackAction(TtsPlaybackService.ACTION_SEEK) {
                                                        putExtra(TtsPlaybackService.EXTRA_INDEX, index)
                                                    }
                                                }
                                            )
                                        },
                                    color = if (isHighlighted) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Compact 2-Row Control Box ──────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        // Row 1: Prev | Play/Pause | Next
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                sendPlaybackAction(TtsPlaybackService.ACTION_PREV)
                            }) { Text("⏮ Prev", fontSize = 14.sp) }

                            Button(
                                onClick = {
                                    val targetState = !isPlaying
                                    isPlaying = targetState // Optimistic UI update
                                    if (!targetState) {
                                        sendPlaybackAction(TtsPlaybackService.ACTION_PAUSE)
                                    } else {
                                        sendPlaybackAction(TtsPlaybackService.ACTION_PLAY) {
                                            putExtra(TtsPlaybackService.EXTRA_TEXT, extractedText)
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text(if (isPlaying) "⏸ Pause" else "▶ Play", fontSize = 14.sp)
                            }

                            TextButton(onClick = {
                                sendPlaybackAction(TtsPlaybackService.ACTION_NEXT)
                            }) { Text("Next ⏭", fontSize = 14.sp) }
                        }

                        // Row 2: Speed label + Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${"%.1f".format(playbackSpeed)}x",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                            Slider(
                                value = playbackSpeed,
                                onValueChange = { newSpeed ->
                                    playbackSpeed = newSpeed
                                    sendPlaybackAction(TtsPlaybackService.ACTION_SET_SPEED) {
                                        putExtra(TtsPlaybackService.EXTRA_SPEED, newSpeed)
                                    }
                                },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ── URL Input Dialog ────────────────────────────────────────
        if (showUrlDialog) {
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                title = { Text("Input URL") },
                text = {
                    OutlinedTextField(
                        value = dialogUrlInput,
                        onValueChange = { dialogUrlInput = it },
                        label = { Text("Paste URL here") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showUrlDialog = false
                        if (dialogUrlInput.isNotEmpty()) {
                            coroutineScope.launch {
                                isLoading = true
                                extractedText = ArticleExtractor.extractTextFromUrl(dialogUrlInput)
                                chunks = extractedText.split("\n").filter { it.trim().isNotEmpty() }
                                isLoading = false
                            }
                        }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
