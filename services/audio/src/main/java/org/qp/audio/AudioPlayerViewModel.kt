package org.qp.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class AudioPlayerViewModel(
    private val audioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val mediaPlayers = ConcurrentHashMap<Uri, MediaPlayer>()
    private var isPaused = false
    var isSoundEnabled = true

    fun isPlayingFile(fileUri: Uri?): Boolean {
        return fileUri != null && fileUri != Uri.EMPTY && mediaPlayers.containsKey(fileUri)
    }

    private fun runOnAudioContext(block: suspend () -> Unit): Job =
        viewModelScope.launch(audioDispatcher) {
            block()
        }

    fun resume() {
        if (!isSoundEnabled || !isPaused) return
        runOnAudioContext {
            isPaused = false
            mediaPlayers.values.filterNot { it.isPlaying }.forEach { it.start() }
        }
    }

    fun pause() {
        if (isPaused) return
        runOnAudioContext {
            isPaused = true
            mediaPlayers.values.filter { it.isPlaying }.forEach { it.pause() }
        }
    }

    fun playFile(
        context: Context,
        soundFileUri: Uri,
        volume: Int
    ) {
        runOnAudioContext {
            val mediaPlayerVolume = volume / 100f

            try {
                val player = mediaPlayers.getOrPut(soundFileUri) {
                    createNewSound(context, soundFileUri)
                }

                player.setVolume(mediaPlayerVolume, mediaPlayerVolume)

                if (isSoundEnabled && !isPaused) {
                    player.start()
                }
            } catch (e: IOException) {
                Log.e("AudioPlayerViewModel", "Failed to prepare MediaPlayer for $soundFileUri", e)
                mediaPlayers.remove(soundFileUri)
            }
        }
    }

    @Throws(IOException::class)
    private fun createNewSound(
        context: Context,
        filePath: Uri
    ): MediaPlayer {
        return MediaPlayer().apply {
            setDataSource(context, filePath)
            prepare()
            setOnCompletionListener { player ->
                runOnAudioContext {
                    mediaPlayers.remove(filePath)
                    player.release()
                }
            }
        }
    }

    fun closeFile(filePath: Uri) {
        runOnAudioContext {
            mediaPlayers.remove(filePath)?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        }
    }

    fun closeAllFiles() {
        runOnAudioContext {
            mediaPlayers.values.forEach { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayers.clear()
        }
    }

    override fun onCleared() {
        closeAllFiles()
        super.onCleared()
    }
}