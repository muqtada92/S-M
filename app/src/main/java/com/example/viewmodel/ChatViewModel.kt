package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioManager
import com.example.data.ChatDatabase
import com.example.data.ChatPreferences
import com.example.data.Message
import com.example.data.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ChatViewModel(private val context: Context) : ViewModel() {

    private val database = ChatDatabase.getDatabase(context)
    private val repository = ChatRepository(database.chatDao)
    private val audioManager = AudioManager(context)

    // Reactive lists of messages & user preferences
    val messages: StateFlow<List<Message>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val preferences: StateFlow<ChatPreferences> = repository.preferences
        .map { it ?: ChatPreferences() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatPreferences()
        )

    // One-on-one current user toggle ("user_a" representing Muqtada, "user_b" representing Sarah)
    val currentUser = MutableStateFlow("user_a")

    // Voice recording states
    val isRecording = MutableStateFlow(false)
    val recordingDuration = MutableStateFlow(0L) // in milliseconds
    private var recordingJob: Job? = null
    private var simulatedFilePath: String? = null

    // Voice playback states
    val activePlayingVoicePath = MutableStateFlow<String?>(null)
    val activePlayingProgress = MutableStateFlow(0f) // from 0.0 to 1.0
    private var playbackJob: Job? = null

    // Real-time synchronization state fields
    private var syncJob: Job? = null
    val isInternetConnected = MutableStateFlow(true)

    // Initialize with mock prefilled chat on first launch so the user gets a warm and friendly chat interface!
    init {
        // Collect preferences and manage synchronization listener
        viewModelScope.launch {
            preferences.collect { prefs ->
                if (currentUser.value != prefs.selfUserId) {
                    currentUser.value = prefs.selfUserId
                }
                if (prefs.syncRoomCode.isNotEmpty()) {
                    startSyncListener(prefs.syncRoomCode)
                } else {
                    syncJob?.cancel()
                    syncJob = null
                }
            }
        }

        viewModelScope.launch {
            repository.allMessages.collect { list ->
                if (list.isEmpty()) {
                    prefillSampleChat()
                }
            }
        }
    }

    private suspend fun prefillSampleChat() {
        val now = System.currentTimeMillis()
        val may2Date = 1683017100000L // May 2, 2023
        val list = listOf(
            Message(
                senderId = "system_log", 
                content = "You created group \"WABetaInfo\"", 
                timestamp = may2Date, 
                messageType = "SYSTEM_LOG", 
                messageUuid = "whatsapp-system-1"
            ),
            Message(
                senderId = "user_b", 
                content = "WABetaInfo", 
                timestamp = may2Date + 5000L, 
                messageType = "TEXT", 
                messageUuid = "whatsapp-message-1"
            ),
            Message(
                senderId = "user_a", 
                content = "Hey there! This chat replicates the exact interface from the WABetaInfo screenshots! Tap the Dark/Light Mode toggle at the top bar to test both Light and Dark WhatsApp themes. 🚀",
                timestamp = now - 50000L,
                messageType = "TEXT",
                messageUuid = "sample-uuid-2"
            )
        )
        for (msg in list) {
            repository.insertMessage(msg)
        }
    }

    fun toggleCurrentUser() {
        viewModelScope.launch {
            val newUser = if (currentUser.value == "user_a") "user_b" else "user_a"
            currentUser.value = newUser
            val currentPrefs = preferences.value
            repository.savePreferences(currentPrefs.copy(selfUserId = newUser))
        }
    }

    fun sendMessage(content: String, messageType: String = "TEXT", mediaUri: String? = null, durationMs: Long = 0L) {
        viewModelScope.launch {
            val sender = currentUser.value
            val message = Message(
                senderId = sender,
                content = content,
                messageType = messageType,
                mediaUri = mediaUri,
                durationMs = durationMs,
                messageUuid = UUID.randomUUID().toString()
            )
            repository.insertMessage(message)

            // Broadcast message if synchronization is active
            val room = preferences.value.syncRoomCode
            if (room.isNotEmpty()) {
                broadcastMessage(message, room)
            }
        }
    }

    fun updateSyncRoomCode(roomCode: String) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            repository.savePreferences(currentPrefs.copy(syncRoomCode = roomCode.trim()))
        }
    }

    fun updateSelfUserId(userId: String) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            repository.savePreferences(currentPrefs.copy(selfUserId = userId))
        }
    }

    // Real-time server stream listener for pairing
    private fun startSyncListener(roomCode: String) {
        syncJob?.cancel()
        if (roomCode.isEmpty()) return

        syncJob = viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            while (syncJob?.isActive == true) {
                try {
                    val request = Request.Builder()
                        .url("https://ntfy.sh/$roomCode/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            isInternetConnected.value = false
                            delay(5000)
                            return@use
                        }
                        isInternetConnected.value = true
                        val body = response.body ?: return@use
                        val reader = body.charStream().buffered()
                        var line = reader.readLine()
                        while (line != null && syncJob?.isActive == true) {
                            try {
                                if (line.trim().isNotEmpty()) {
                                    handleIncomingNtfyLine(line)
                                }
                            } catch (e: Exception) {
                                Log.e("ChatViewModel", "Sync parsing error: ${e.message}")
                            }
                            line = reader.readLine()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Sync network disconnect, retrying: ${e.message}")
                    isInternetConnected.value = false
                    delay(5000)
                }
            }
        }
    }

    private suspend fun handleIncomingNtfyLine(line: String) {
        val json = JSONObject(line)
        val event = json.optString("event")
        if (event == "message") {
            val messageBodyString = json.optString("message")
            if (messageBodyString.isNotEmpty() && messageBodyString.startsWith("{")) {
                val payload = JSONObject(messageBodyString)
                val msgSenderId = payload.optString("senderId")
                val msgContent = payload.optString("content")
                val msgTimestamp = payload.optLong("timestamp")
                val msgType = payload.optString("messageType")
                val msgMediaUri = payload.optString("mediaUri", "")
                val msgDurationMs = payload.optLong("durationMs", 0L)
                val msgUuid = payload.optString("messageUuid")

                val mySelfId = preferences.value.selfUserId
                if (msgSenderId != mySelfId) {
                    val alreadyExists = messages.value.any { 
                        it.messageUuid == msgUuid || (it.timestamp == msgTimestamp && it.senderId == msgSenderId) 
                    }
                    if (!alreadyExists) {
                        val incomingMsg = Message(
                            senderId = msgSenderId,
                            content = msgContent,
                            timestamp = msgTimestamp,
                            messageType = msgType,
                            mediaUri = if (msgMediaUri.isEmpty()) null else msgMediaUri,
                            durationMs = msgDurationMs,
                            messageUuid = msgUuid
                        )
                        repository.insertMessage(incomingMsg)
                    }
                }
            }
        }
    }

    private fun broadcastMessage(msg: Message, roomCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var remoteMediaUri = msg.mediaUri
                if (remoteMediaUri != null && !remoteMediaUri.startsWith("http")) {
                    remoteMediaUri = uploadFileToNtfy(remoteMediaUri, roomCode)
                    if (remoteMediaUri != null) {
                        repository.insertMessage(msg.copy(mediaUri = remoteMediaUri))
                    }
                }

                val payload = JSONObject().apply {
                    put("senderId", msg.senderId)
                    put("content", msg.content)
                    put("timestamp", msg.timestamp)
                    put("messageType", msg.messageType)
                    put("mediaUri", remoteMediaUri ?: "")
                    put("durationMs", msg.durationMs)
                    put("messageUuid", msg.messageUuid)
                }

                val client = OkHttpClient()
                val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://ntfy.sh/$roomCode")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("ChatViewModel", "Broadcast HTTP failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error broadcasting message: ${e.message}")
            }
        }
    }

    private suspend fun uploadFileToNtfy(localPath: String?, roomCode: String): String? {
        if (localPath == null) return null
        if (localPath.startsWith("http://") || localPath.startsWith("https://")) return localPath

        return withContext(Dispatchers.IO) {
            try {
                val file = if (localPath.startsWith("content://")) {
                    val tempFile = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}")
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(localPath)) ?: return@withContext null
                    FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
                    tempFile
                } else {
                    File(localPath)
                }

                if (!file.exists()) return@withContext null

                val client = OkHttpClient()
                val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://ntfy.sh")
                    .put(requestBody)
                    .addHeader("X-File", file.name)
                    .addHeader("X-Filename", file.name)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        val attachment = json.optJSONObject("attachment")
                        val remoteUrl = attachment?.optString("url")
                        if (localPath.startsWith("content://")) {
                            file.delete()
                        }
                        remoteUrl
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to upload file to ntfy: ${e.message}")
                null
            }
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // Customization operations
    fun updateSentColor(hex: String) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            repository.savePreferences(currentPrefs.copy(sentColorHex = hex))
        }
    }

    fun updateReceivedColor(hex: String) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            repository.savePreferences(currentPrefs.copy(receivedColorHex = hex))
        }
    }

    fun updateBackgroundPreset(presetId: String) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            repository.savePreferences(currentPrefs.copy(
                backgroundType = "PRESET",
                presetId = presetId
            ))
        }
    }

    fun updateSolidColor(hex: String) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            repository.savePreferences(currentPrefs.copy(
                backgroundType = "SOLID",
                solidColorHex = hex
            ))
        }
    }

    fun updateCustomBackground(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val savedPath = saveMediaLocally(uri, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
            if (savedPath != null) {
                val currentPrefs = preferences.value
                repository.savePreferences(currentPrefs.copy(
                    backgroundType = "GALLERY",
                    customBackgroundUri = savedPath
                ))
            }
        }
    }

    // Copy selected external media to private files directory to persist it forever
    fun saveMediaLocally(uri: Uri, destFileName: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outFile = File(context.filesDir, destFileName)
            FileOutputStream(outFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            outFile.absolutePath
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to save selected media locally: ${e.message}")
            null
        }
    }

    // Voice note recording sequence
    fun startRecordingVoice() {
        isRecording.value = true
        recordingDuration.value = 0L
        simulatedFilePath = audioManager.startRecording()

        recordingJob = viewModelScope.launch {
            while (isRecording.value) {
                delay(100)
                recordingDuration.value += 100L
            }
        }
    }

    fun stopRecordingVoice(cancelled: Boolean) {
        if (!isRecording.value) return
        isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        val isSimulator = simulatedFilePath?.startsWith("SIMULATED_VOICE") == true
        val (finalPath, duration) = audioManager.stopRecording(isSimulator)

        if (!cancelled && duration > 500L) {
            sendMessage(
                content = "Voice Message (${duration / 1000}s)",
                messageType = "VOICE",
                mediaUri = finalPath,
                durationMs = duration
            )
        }
    }

    // Voice note playback handling
    fun toggleVoicePlay(messageId: Long, audioPath: String, durationMs: Long) {
        if (activePlayingVoicePath.value == audioPath) {
            // Already playing this model -> stop
            stopVoicePlay()
        } else {
            // Stop any active playing note
            stopVoicePlay()

            activePlayingVoicePath.value = audioPath
            if (audioPath.contains("SIMULATED_VOICE")) {
                // Run simulation
                runVoiceSimulation(durationMs)
            } else {
                audioManager.startPlayback(audioPath) {
                    stopVoicePlay()
                }
                // Monitor active playing progression
                playbackJob = viewModelScope.launch {
                    val start = System.currentTimeMillis()
                    while (activePlayingVoicePath.value == audioPath) {
                        val elapsed = System.currentTimeMillis() - start
                        val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                        activePlayingProgress.value = progress
                        if (progress >= 1.0f) {
                            stopVoicePlay()
                            break
                        }
                        delay(50)
                    }
                }
            }
        }
    }

    private fun runVoiceSimulation(durationMs: Long) {
        playbackJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                activePlayingProgress.value = progress
                if (progress >= 1f) {
                    stopVoicePlay()
                    break
                }
                delay(50)
            }
        }
    }

    fun stopVoicePlay() {
        audioManager.stopPlayback()
        activePlayingVoicePath.value = null
        activePlayingProgress.value = 0f
        playbackJob?.cancel()
        playbackJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopVoicePlay()
        audioManager.stopPlayback()
    }
}

class ChatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
