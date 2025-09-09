package org.qp.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Runnable
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.Volatile
import kotlin.concurrent.thread

class AudioPlayerService : ViewModel() {

    private val sounds = ConcurrentHashMap<Uri, MediaPlayer>()
    private var audioThread: Thread? = null
    private val audioQueue = LinkedBlockingQueue<Runnable>()
    @Volatile private var audioThreadInit = false
    @Volatile private var isPaused = false

    @Volatile var soundEnabled = true

    fun isPlayingFile(fileUri: Uri?): Boolean {
        return fileUri != null && fileUri != Uri.EMPTY && sounds.containsKey(fileUri)
    }

    private fun getSystemVolume(volume: Int): Float {
        return volume / 100f
    }

    private fun runOnAudioThread(runnable: Runnable) {
        if (audioThreadInit) {
            audioQueue.offer(runnable)
        } else {
            Log.e(javaClass.simpleName, "runOnAudioThread: THREAD NOT INIT!")
        }
    }

    fun start() {
        isPaused = false
        if (!audioThreadInit) {
            audioThread = thread(name = "audioThread") {
                audioThreadInit = true
                while (!Thread.currentThread().isInterrupted) {
                    audioQueue.poll()?.run()
                }
            }
        }
    }

    fun resume() {
        if (!soundEnabled) return
        if (!isPaused) return
        runOnAudioThread {
            isPaused = false
            sounds.values.stream()
                .filter { player -> !player.isPlaying }
                .forEach { obj -> obj.start() }
        }
    }

    fun pause() {
        if (isPaused) return
        runOnAudioThread {
            isPaused = true
            sounds.values.stream()
                .filter { player -> player.isPlaying }
                .forEach { obj -> obj.pause() }
        }
    }

    override fun onCleared() {
        closeAllFiles()

        audioThreadInit = false
        audioThread?.interrupt()
    }

    fun playFile(
        context: Context,
        soundFileUri: Uri,
        volume: Int
    ) {
        runOnAudioThread {
            val sound = sounds.get(soundFileUri)
            val sysVolume = getSystemVolume(volume)
            if (sound == null) {
                val newSound = createNewSound(context, soundFileUri, sysVolume)
                if (soundEnabled && !isPaused) {
                    if (!newSound.isPlaying) {
                        newSound.start()
                    }
                }
            } else {
                sound.setVolume(sysVolume, sysVolume)
                if (soundEnabled && !isPaused) {
                    if (!sound.isPlaying) {
                        sound.start()
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createNewSound(
        context: Context,
        filePath: Uri,
        sysVolume: Float
    ): MediaPlayer {
        return MediaPlayer().apply {
            setDataSource(context, filePath)
            prepare()
            setOnCompletionListener { player ->
                sounds.remove(filePath)
                player.reset()
                player.release()
            }
            setVolume(sysVolume, sysVolume)
            sounds.put(filePath, this)
        }
    }

    fun closeAllFiles() {
        runOnAudioThread {
            sounds.values.stream()
                .filter(Objects::nonNull)
                .forEach { player ->
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.reset()
                    player.release()
                }
            sounds.clear()
        }
    }

    fun closeFile(filePath: Uri) {
        if (sounds.containsKey(filePath)) {
            runOnAudioThread {
                val player = sounds.get(filePath)
                if (player != null) {
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.reset()
                    player.release()
                }
                sounds.remove(filePath)
            }
        }
    }
}