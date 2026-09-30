package com.example.musicplayer

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.media3.exoplayer.ExoPlayer

data class Song(val title: String, val uri: Uri, val duration: Long)

class MainActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer
    private var pendingLoad: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) pendingLoad?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        setContent {
            MaterialTheme {
                MusicPlayerApp(
                    player = player,
                    onRequestPermission = { requestPermissions() },
                    onLoadSongs = { loadSongs() }
                )
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33)
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
        else
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        perms.add(Manifest.permission.RECORD_AUDIO)
        permissionLauncher.launch(perms.toTypedArray())
    }

    fun loadSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null, null, null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )
        cursor?.use {
            val titleCol = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idCol = it.getColumnIndex(MediaStore.Audio.Media._ID)
            val durCol = it.getColumnIndex(MediaStore.Audio.Media.DURATION)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()
                )
                songs.add(Song(it.getString(titleCol) ?: "Unknown", uri, it.getLong(durCol)))
            }
        }
        return songs
    }
}
