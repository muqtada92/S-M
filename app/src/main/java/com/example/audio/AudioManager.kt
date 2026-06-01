package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordFile: File? = null

    fun startRecording(): String? {
        return try {
            val file = File(context.filesDir, "voice_rec_${System.currentTimeMillis()}.3gp")
            currentRecordFile = file

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = MediaRecorder(context).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
            } else {
                @Suppress("DEPRECATION")
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("AudioManager", "Error starting hardware MediaRecorder: ${e.message}. Switching to simulated recorder.")
            "SIMULATED_VOICE_${System.currentTimeMillis()}.3gp"
        }
    }

    fun stopRecording(isSimulated: Boolean): Pair<String, Long> {
        val timestamp = System.currentTimeMillis()
        if (isSimulated) {
            return Pair("SIMULATED_VOICE_$timestamp.3gp", 5200L) // Simulate a cute 5.2 seconds clip
        }
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val path = currentRecordFile?.absolutePath ?: "SIMULATED_VOICE_$timestamp.3gp"
            val duration = getPlayableDuration(path)
            Pair(path, duration)
        } catch (e: Exception) {
            Log.e("AudioManager", "Error stopping hardware recorder: ${e.message}")
            mediaRecorder = null
            Pair("SIMULATED_VOICE_$timestamp.3gp", 6000L)
        }
    }

    fun startPlayback(audioPath: String, onComplete: () -> Unit) {
        if (audioPath.startsWith("SIMULATED_VOICE")) {
            // Handled as visual simulation directly in the Composable view state,
            // but we can still register onComplete trigger in simulated thread if needed.
            return
        }
        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    onComplete()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioManager", "Error starting hardware playback: ${e.message}")
            onComplete()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AudioManager", "Error stopping hardware playback: ${e.message}")
        }
    }

    private fun getPlayableDuration(path: String): Long {
        if (path.startsWith("SIMULATED_VOICE")) return 5000L
        var mp: MediaPlayer? = null
        return try {
            mp = MediaPlayer()
            mp.setDataSource(path)
            mp.prepare()
            val duration = mp.duration.toLong()
            mp.release()
            duration
        } catch (e: Exception) {
            mp?.release()
            3500L // Default duration fallback
        }
    }
}
