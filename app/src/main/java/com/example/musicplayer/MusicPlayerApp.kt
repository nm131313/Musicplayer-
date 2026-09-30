package com.example.musicplayer

import android.media.audiofx.Visualizer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

val themes = listOf(
    "بنفش" to Color(0xFF6750A4),
    "آبی" to Color(0xFF0061A4),
    "سبز" to Color(0xFF006E1C),
    "قرمز" to Color(0xFFBA1A1A),
    "نارنجی" to Color(0xFF8B5000)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerApp(
    player: ExoPlayer,
    onRequestPermission: () -> Unit,
    onLoadSongs: () -> List<Song>
) {
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(themes[0]) }
    var showVisualizer by remember { mutableStateOf(true) }
    var fftData by remember { mutableStateOf<FloatArray?>(null) }
    var visualizer by remember { mutableStateOf<Visualizer?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying, player.audioSessionId) {
        if (isPlaying && player.audioSessionId != 0) {
            try {
                visualizer?.release()
                val viz = Visualizer(player.audioSessionId)
                viz.captureSize = Visualizer.getCaptureSizeRange()[1]
                viz.setDataCaptureListener(
                    Visualizer.OnDataCaptureListener { _, fft, _ ->
                        fftData = processFft(fft)
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                viz.enabled = true
                visualizer = viz
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            visualizer?.enabled = false
            visualizer?.release()
            player.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎵 پلیر حرفه‌ای") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = selectedTheme.second,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showVisualizer = !showVisualizer }) {
                        Icon(Icons.Default.GraphicEq, "ویژوالایزر")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(selectedTheme.second.copy(alpha = 0.06f))
        ) {
            Text("انتخاب تم:", Modifier.padding(12.dp, 8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                themes.forEach { theme ->
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(theme.second, MaterialTheme.shapes.small)
                            .clickable { selectedTheme = theme }
                    )
                }
            }

            if (showVisualizer) {
                VisualizerView(
                    fftData = fftData,
                    color = selectedTheme.second,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.85f),
                            MaterialTheme.shapes.medium)
                )
            }

            currentSong?.let { song ->
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = selectedTheme.second.copy(alpha = 0.15f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isPlaying) "▶️ در حال پخش" else "⏸️ متوقف",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.seekToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "قبلی",
                        Modifier.size(40.dp), tint = selectedTheme.second)
                }
                FilledIconButton(
                    onClick = { if (isPlaying) player.pause() else player.play() },
                    modifier = Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = selectedTheme.second
                    )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "پخش", Modifier.size(36.dp), tint = Color.White
                    )
                }
                IconButton(onClick = { player.seekToNext() }) {
                    Icon(Icons.Default.SkipNext, "بعدی",
                        Modifier.size(40.dp), tint = selectedTheme.second)
                }
            }

            HorizontalDivider()

            if (songs.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LibraryMusic, null,
                        Modifier.size(72.dp), tint = selectedTheme.second)
                    Spacer(Modifier.height(16.dp))
                    Text("هیچ آهنگی پیدا نشد")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onRequestPermission()
                            songs = onLoadSongs()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedTheme.second
                        )
                    ) { Text("بارگذاری آهنگ‌ها") }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(songs) { song ->
                        ListItem(
                            headlineContent = { Text(song.title) },
                            leadingContent = {
                                Icon(Icons.Default.MusicNote, null,
                                    tint = selectedTheme.second)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (currentSong == song)
                                    selectedTheme.second.copy(alpha = 0.2f)
                                else Color.Transparent
                            ),
                            modifier = Modifier.clickable {
                                currentSong = song
                                player.setMediaItem(MediaItem.fromUri(song.uri))
                                player.prepare()
                                player.play()
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

fun processFft(fft: ByteArray): FloatArray {
    val n = fft.size
    val magnitudes = FloatArray(n / 2 + 1)
    magnitudes[0] = kotlin.math.abs(fft[0].toFloat())
    if (n > 1) magnitudes[n / 2] = kotlin.math.abs(fft[1].toFloat())
    for (k in 1 until n / 2) {
        val i = k * 2
        val real = fft[i].toFloat()
        val imag = fft[i + 1].toFloat()
        magnitudes[k] = kotlin.math.sqrt(real * real + imag * imag)
    }
    return magnitudes
}
