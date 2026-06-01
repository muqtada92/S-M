package com.example.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ChatPreferences
import com.example.data.Message
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.VideoView
import android.widget.MediaController
import android.net.Uri

// Color presets for custom bubbles
val SentColorPresets = listOf(
    "#00B0FF" to "Ocean Blue",
    "#8E24AA" to "Aura Purple",
    "#2E7D32" to "Sage Forest",
    "#F57C00" to "Sunset Amber",
    "#C2185B" to "Velvet Rose",
    "#00E5FF" to "Cool Cyan",
    "#E91E63" to "Barbie Pink",
    "#4CAF50" to "Fresh Green"
)

val ReceivedColorPresets = listOf(
    "#303030" to "Coal Black",
    "#455A64" to "Slate Grey",
    "#4A154B" to "Deep Plum",
    "#1E293B" to "Slate Dark",
    "#3E2723" to "Chestnut",
    "#1B5E20" to "Forest Shadow",
    "#311B92" to "Night Violet"
)

val SolidBgPresets = listOf(
    "#090D16" to "Obsidian",
    "#121212" to "Dark Mode",
    "#1A365D" to "Navy",
    "#2A1B3D" to "Aubergine",
    "#1E352F" to "Deep Pine"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val messageList by viewModel.messages.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    val isRecordingState by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDurationVal by viewModel.recordingDuration.collectAsStateWithLifecycle()
    
    val playingVoiceStatePath by viewModel.activePlayingVoicePath.collectAsStateWithLifecycle()
    val playingVoiceProgressState by viewModel.activePlayingProgress.collectAsStateWithLifecycle()
    val isInternetConnected by viewModel.isInternetConnected.collectAsStateWithLifecycle()

    var showCustomizerPanel by remember { mutableStateOf(false) }
    var textInputState by remember { mutableStateOf("") }
    
    // Popup Fullscreen Modals
    var activeFullscreenPhotoPath by remember { mutableStateOf<String?>(null) }
    var activeFullscreenVideoPath by remember { mutableStateOf<String?>(null) }
    
    // Lazy list scrolling
    val listState = rememberLazyListState()
    
    // Local state for checking permission
    var hasMicPermission by remember { mutableStateOf(false) }
    
    // Switch between WhatsApp light and dark mode side-by-side versions dynamically!
    var isWhatsAppDark by remember { mutableStateOf(false) }

    // Sync WhatsApp Light vs Dark mode styling with database preferences
    LaunchedEffect(isWhatsAppDark) {
        if (isWhatsAppDark) {
            viewModel.updateBackgroundPreset("preset_whatsapp_dark")
            viewModel.updateSentColor("#005C4B")
            viewModel.updateReceivedColor("#202C33")
        } else {
            viewModel.updateBackgroundPreset("preset_whatsapp_light")
            viewModel.updateSentColor("#D9FDD3")
            viewModel.updateReceivedColor("#FFFFFF")
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasMicPermission = granted
            if (granted) {
                viewModel.startRecordingVoice()
            } else {
                viewModel.startRecordingVoice()
            }
        }
    )

    // Launch Single Photo picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val savedLocally = viewModel.saveMediaLocally(uri, "img_${System.currentTimeMillis()}.jpg")
                if (savedLocally != null) {
                    viewModel.sendMessage(
                        content = "Shared Photo",
                        messageType = "PHOTO",
                        mediaUri = savedLocally
                    )
                }
            }
        }
    )

    // Launch Single Video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val savedLocally = viewModel.saveMediaLocally(uri, "vid_${System.currentTimeMillis()}.mp4")
                if (savedLocally != null) {
                    viewModel.sendMessage(
                        content = "Shared Video",
                        messageType = "VIDEO",
                        mediaUri = savedLocally
                    )
                }
            }
        }
    )

    // Background custom image wallpaper selector
    val customWallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateCustomBackground(uri)
            }
        }
    )

    // Auto-scroll to latest chat index whenever a new message lands
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.size - 1)
        }
    }

    // Outer responsive smartphone wrapper to adjust dimensions exactly resembling a WhatsApp phone mockup on larger views!
    SmartphoneFrame(isDark = isWhatsAppDark) {
        Scaffold(
            topBar = {
                WhatsAppTopAppBar(
                    isDark = isWhatsAppDark,
                    onToggleDark = { isWhatsAppDark = !isWhatsAppDark },
                    showCustomizerPanel = showCustomizerPanel,
                    onToggleCustomizer = { showCustomizerPanel = !showCustomizerPanel },
                    onClearChat = { viewModel.clearChat() }
                )
            },
            bottomBar = {
                Column {
                    if (showCustomizerPanel) {
                        CustomizerUI(
                            preferences = preferences,
                            onSentColorSelected = viewModel::updateSentColor,
                            onReceivedColorSelected = viewModel::updateReceivedColor,
                            onPresetSelected = viewModel::updateBackgroundPreset,
                            onSolidSelected = viewModel::updateSolidColor,
                            onUploadCustomBg = {
                                customWallpaperPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onSyncRoomCodeChanged = viewModel::updateSyncRoomCode,
                            onSelfUserIdChanged = viewModel::updateSelfUserId,
                            isInternetConnected = isInternetConnected
                        )
                    }

                    WhatsAppInputArea(
                        text = textInputState,
                        onValueChange = { textInputState = it },
                        isDark = isWhatsAppDark,
                        onSendText = {
                            viewModel.sendMessage(textInputState)
                            textInputState = ""
                        },
                        onMicClick = {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onAddClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCameraClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        isRecording = isRecordingState,
                        recordingDuration = recordingDurationVal,
                        onCancelRecord = { viewModel.stopRecordingVoice(cancelled = true) },
                        onSaveRecord = { viewModel.stopRecordingVoice(cancelled = false) }
                    )
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Renders beautifully sharp custom vector doodle background according to light or dark state!
                ChatBackground(
                    backgroundType = preferences.backgroundType,
                    presetId = preferences.presetId,
                    customBgUri = preferences.customBackgroundUri,
                    solidColorHex = preferences.solidColorHex
                )

                // Render logs / bubbles
                if (messageList.isEmpty()) {
                    EmptyStateView()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var lastDateString = ""
                        
                        messageList.forEachIndexed { index, msg ->
                            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                            val msgDateString = sdf.format(Date(msg.timestamp))

                            if (msgDateString != lastDateString) {
                                lastDateString = msgDateString
                                val displayDate = getDisplayDateString(msg.timestamp)
                                item(key = "date_sep_${msg.id}_$index") {
                                    DateHeaderPill(text = displayDate, isDark = isWhatsAppDark)
                                }
                            }

                            item(key = msg.id) {
                                val isMe = msg.senderId == currentUser
                                
                                if (msg.messageType == "SYSTEM_LOG") {
                                    SystemGroupLogPill(content = msg.content, isDark = isWhatsAppDark)
                                } else {
                                    WhatsAppMessageBubble(
                                        message = msg,
                                        isMe = isMe,
                                        isDark = isWhatsAppDark,
                                        isPlaying = playingVoiceStatePath == msg.mediaUri,
                                        voiceProgress = playingVoiceProgressState,
                                        onVoicePlayClick = {
                                            msg.mediaUri?.let { path ->
                                                viewModel.toggleVoicePlay(msg.id, path, msg.durationMs)
                                            }
                                        },
                                        onPhotoTap = { path -> activeFullscreenPhotoPath = path },
                                        onVideoTap = { path -> activeFullscreenVideoPath = path },
                                        onDeleteRequest = { viewModel.deleteMessage(msg.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Fullscreen Photo viewer implementation
    if (activeFullscreenPhotoPath != null) {
        Dialog(onDismissRequest = { activeFullscreenPhotoPath = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { activeFullscreenPhotoPath = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = activeFullscreenPhotoPath,
                    contentDescription = "Expanded Visual Sharing View",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { activeFullscreenPhotoPath = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit photo modal",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // Modal Fullscreen Video viewer player implementation
    if (activeFullscreenVideoPath != null) {
        Dialog(onDismissRequest = { activeFullscreenVideoPath = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sleek Video Player", color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { activeFullscreenVideoPath = null }) {
                            Icon(Icons.Default.Close, "Dismiss Video", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    ) {
                        val currentPath = activeFullscreenVideoPath!!
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    val uri = if (currentPath.startsWith("http://") || currentPath.startsWith("https://")) {
                                        Uri.parse(currentPath)
                                    } else if (currentPath.startsWith("android.resource")) {
                                        Uri.parse(currentPath)
                                    } else {
                                        Uri.fromFile(File(currentPath))
                                    }
                                    setVideoURI(uri)
                                    val controller = MediaController(context)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    start()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRow(
    message: Message,
    isMe: Boolean,
    preferences: ChatPreferences,
    isPlaying: Boolean,
    voiceProgress: Float,
    onVoicePlayClick: () -> Unit,
    onPhotoTap: (String) -> Unit,
    onVideoTap: (String) -> Unit,
    onDeleteRequest: () -> Unit
) {
    val dateString = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    // Resolve bubble accent background color
    val bubbleColorString = if (isMe) preferences.sentColorHex else preferences.receivedColorHex
    val bubbleColor = remember(bubbleColorString) {
        try {
            Color(android.graphics.Color.parseColor(bubbleColorString))
        } catch (e: Exception) {
            if (isMe) Color(0xFF006A6A) else Color(0xFFFFFFFF)
        }
    }

    val contentColor = remember(bubbleColor) {
        // High contrast formula to support both dark accents (Teal) and pure white bubbles correctly
        val red = bubbleColor.red
        val green = bubbleColor.green
        val blue = bubbleColor.blue
        val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
        if (luminance > 0.6) Color(0xFF1D1B20) else Color.White
    }

    var showDeleteOption by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        // Partner initials avatars
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFFEADDFF), CircleShape)
                    .align(Alignment.Bottom)
                    .border(BorderStroke(1.dp, Color(0xFFD0BCFF)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("P", color = Color(0xFF21005D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // Displays contact nickname helper above message
            Text(
                text = if (isMe) "You" else "Partner",
                fontSize = 11.sp,
                color = if (isMe) Color(0xFF6750A4) else Color(0xFF49454F),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Double clickable bubble message card that unveils deletion options
            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                shape = RoundedCornerShape(
                    topStart = if (isMe) 24.dp else 0.dp,
                    topEnd = if (isMe) 0.dp else 24.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                ),
                border = if (!isMe) BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f)) else null,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = {
                            if (message.messageType == "PHOTO") {
                                message.mediaUri?.let { onPhotoTap(it) }
                            } else if (message.messageType == "VIDEO") {
                                message.mediaUri?.let { onVideoTap(it) }
                            }
                        },
                        onLongClick = { showDeleteOption = true }
                    )
                    .testTag("chat_bubble_card_${message.id}"),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isMe) 3.dp else 1.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    when (message.messageType) {
                        "TEXT" -> {
                            Text(
                                text = message.content,
                                color = contentColor,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                        "PHOTO" -> {
                            Column {
                                message.mediaUri?.let { path ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = path,
                                            contentDescription = "Shared photo stream preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .size(36.dp)
                                                .align(Alignment.Center),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Expand photo magnifier",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to enlarge photo",
                                    color = contentColor.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        "VIDEO" -> {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Play media attachment preview",
                                        tint = Color.White,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Share MP4 Video Clip",
                                    color = contentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        "VOICE" -> {
                            VoiceMessageBubble(
                                isPlaying = isPlaying,
                                progress = voiceProgress,
                                durationMs = message.durationMs,
                                contentColor = contentColor,
                                onPlayClick = onVoicePlayClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Timestamp & Status display
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateString,
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Delivered status checkmark",
                                tint = if (bubbleColor == Color(0xFF006A6A)) Color(0xFF80CBC4) else contentColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showDeleteOption) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDeleteRequest()
                            showDeleteOption = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Icon(Icons.Default.Delete, "Remove message icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showDeleteOption = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceMessageBubble(
    isPlaying: Boolean,
    progress: Float,
    durationMs: Long,
    contentColor: Color = Color.White,
    onPlayClick: () -> Unit
) {
    val durationSecs = durationMs / 1000
    val durationText = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Toggle play audio button
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier
                .background(contentColor.copy(alpha = 0.15f), CircleShape)
                .size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Toggle playback voice message note",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Soundwave progress display
        Column(modifier = Modifier.weight(1f)) {
            // Live pulsing sound visualizer bars if playing!
            if (isPlaying) {
                VoiceEqualizerWaves(color = contentColor)
            } else {
                StaticSoundwaves(color = contentColor)
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            // Smooth Slider showing playback position
            LinearProgressIndicator(
                progress = { progress },
                color = contentColor,
                trackColor = contentColor.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = durationText,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VoiceEqualizerWaves(color: Color = Color.White) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    
    // Wave heights mapped dynamically
    val d1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(250, easing = LinearEasing), RepeatMode.Reverse), label = "h1"
    )
    val d2 by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(330, easing = LinearEasing), RepeatMode.Reverse), label = "h2"
    )
    val d3 by infiniteTransition.animateFloat(
        initialValue = 3f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(210, easing = LinearEasing), RepeatMode.Reverse), label = "h3"
    )
    val d4 by infiniteTransition.animateFloat(
        initialValue = 16f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(290, easing = LinearEasing), RepeatMode.Reverse), label = "h4"
    )

    Row(
        modifier = Modifier.height(20.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(d1, d2, d3, d4, d1*0.8f, d3*1.1f, d2*0.6f, d4*0.9f).forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun StaticSoundwaves(color: Color = Color.White) {
    Row(
        modifier = Modifier.height(20.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(6, 12, 14, 8, 12, 16, 10, 4, 8, 14, 6, 12).forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(color.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

@Composable
fun PulsingMicIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_mic"
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .background(Color.Red.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.Red.copy(alpha = 0.25f * scale), CircleShape)
        )
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Pulsing recorded sound wave",
            tint = Color.Red,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "No messaging threads loaded",
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Welcome to DuoChat!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A safe, fully custom space built exclusively for you two. Express with voice notes, layouts, and wallpapers.",
                    fontSize = 13.sp,
                    color = Color(0xFF49454F),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun CustomizerUI(
    preferences: ChatPreferences,
    onSentColorSelected: (String) -> Unit,
    onReceivedColorSelected: (String) -> Unit,
    onPresetSelected: (String) -> Unit,
    onSolidSelected: (String) -> Unit,
    onUploadCustomBg: () -> Unit,
    onSyncRoomCodeChanged: (String) -> Unit,
    onSelfUserIdChanged: (String) -> Unit,
    isInternetConnected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F2FA))
            .padding(16.dp)
            .heightIn(max = 480.dp)
            .verticalScroll(rememberScrollState())
            .testTag("customizer_panel")
    ) {
        Text(
            text = "Duo Custom Styling Engine",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1D1B20)
        )
        
        Spacer(modifier = Modifier.height(14.dp))

        // Sender message color choices
        Text(text = "Sent Message Color Bubble", fontSize = 13.sp, color = Color(0xFF49454F), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SentColorPresets.forEach { (hex, label) ->
                val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                val active = preferences.sentColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (active) 3.dp else 0.dp,
                            color = Color(0xFF6750A4),
                            shape = CircleShape
                        )
                        .clickable { onSentColorSelected(hex) }
                        .testTag("sent_color_preset_$label"),
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        val iconTint = if (hex.equals("#FFFFFF", ignoreCase = true)) Color(0xFF1D1B20) else Color.White
                        Icon(Icons.Default.Check, "Active Selection", tint = iconTint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Receiver message color choices
        Text(text = "Received Message Color Bubble", fontSize = 13.sp, color = Color(0xFF49454F), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReceivedColorPresets.forEach { (hex, label) ->
                val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                val active = preferences.receivedColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (active) 3.dp else 0.dp,
                            color = Color(0xFF6750A4),
                            shape = CircleShape
                        )
                        .clickable { onReceivedColorSelected(hex) }
                        .testTag("received_color_preset_$label"),
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        val iconTint = if (hex.equals("#FFFFFF", ignoreCase = true) || hex.equals("#F3EDF7", ignoreCase = true)) Color(0xFF1D1B20) else Color.White
                        Icon(Icons.Default.Check, "Active Selection", tint = iconTint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Background Wallpaper selection
        Text(text = "Preset Wallpapers & Backdrops", fontSize = 13.sp, color = Color(0xFF49454F), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Predefined beautiful wallpapers
            listOf(
                Triple("preset_slate", "Minimal", Color(0xFFFEF7FF)),
                Triple("preset_cosmic", "Space", Color(0xFF0D152D)),
                Triple("preset_sunset", "Sunset", Color(0xFFC72C41)),
                Triple("preset_forest", "Sage", Color(0xFF1D352D)),
                Triple("preset_lavender", "Lavender", Color(0xFF4C3078))
            ).forEach { (id, label, colorHex) ->
                val active = preferences.backgroundType == "PRESET" && preferences.presetId == id
                Card(
                    onClick = { onPresetSelected(id) },
                    colors = CardDefaults.cardColors(containerColor = colorHex),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(width = 86.dp, height = 48.dp)
                        .border(
                            width = if (active) 3.dp else 1.dp,
                            color = if (active) Color(0xFF6750A4) else Color(0xFFCAC4D0).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .testTag("bg_preset_$label")
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = label, 
                            color = if (id == "preset_slate") Color(0xFF1D1B20) else Color.White, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Solid Color Customization options
        Text(text = "Solid Wallpaper Shades", fontSize = 13.sp, color = Color(0xFF49454F), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SolidBgPresets.forEach { (hex, name) ->
                val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                val active = preferences.backgroundType == "SOLID" && preferences.solidColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 34.dp)
                        .background(color, RoundedCornerShape(8.dp))
                        .border(
                            width = if (active) 3.dp else 1.dp,
                            color = if (active) Color(0xFF6750A4) else Color(0xFFCAC4D0).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSolidSelected(hex) }
                        .testTag("bg_solid_$name"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name, 
                        color = if (hex.equals("#FEF7FF", ignoreCase = true) || hex.equals("#F3EDF7", ignoreCase = true)) Color(0xFF1D1B20) else Color.White, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Upload custom image button
        Button(
            onClick = onUploadCustomBg,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4).copy(alpha = 0.12f),
                contentColor = Color(0xFF6750A4)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("upload_custom_bg_button")
        ) {
            Icon(Icons.Default.CloudUpload, "Upload files icon")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Background From Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFCAC4D0).copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pair & Sync Over Internet",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF1D1B20)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Enter the same pairing Room Code on both devices, and assign separate identities to chat in real-time over the internet!",
            fontSize = 11.sp,
            color = Color(0xFF49454F),
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Device identity selector
        Text(text = "App Identity on this Device", fontSize = 13.sp, color = Color(0xFF49454F), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isMuqtada = preferences.selfUserId == "user_a"
            val isSarah = preferences.selfUserId == "user_b"

            Button(
                onClick = { onSelfUserIdChanged("user_a") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMuqtada) Color(0xFF006A6A) else Color(0xFF6750A4).copy(alpha = 0.08f),
                    contentColor = if (isMuqtada) Color.White else Color(0xFF49454F)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Identify as Muqtada", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onSelfUserIdChanged("user_b") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSarah) Color(0xFF006A6A) else Color(0xFF6750A4).copy(alpha = 0.08f),
                    contentColor = if (isSarah) Color.White else Color(0xFF49454F)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Identify as Sarah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Room Code Input
        Text(text = "Pairing Room Code", fontSize = 13.sp, color = Color(0xFF49454F), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = preferences.syncRoomCode,
                onValueChange = onSyncRoomCodeChanged,
                placeholder = { Text("e.g. muqtada_sarah_room", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.weight(1.5f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6750A4),
                    unfocusedBorderColor = Color(0xFFCAC4D0)
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
            )

            Button(
                onClick = {
                    val rand = "duochat-" + (100000..999999).random()
                    onSyncRoomCodeChanged(rand)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6750A4).copy(alpha = 0.08f),
                    contentColor = Color(0xFF6750A4)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Refresh, "Generate code", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Auto-Gen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Connection Status Banner
        val statusText = if (preferences.syncRoomCode.isEmpty()) {
            "⚪ Standalone Local Mode (messages stay on device)"
        } else if (isInternetConnected) {
            "🟢 Internet Paired & Connected — Room: '${preferences.syncRoomCode}'"
        } else {
            "⚠️ Disconnected — Trying to reconnect sync stream..."
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF6750A4).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = statusText,
                fontSize = 11.sp,
                color = Color(0xFF49454F),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ================= ORIGINAL DESIGN ENHANCEMENTS FOR WHATSAPP CLONE MODES =================

@Composable
fun SmartphoneFrame(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val maxW = maxWidth

        val useFrame = maxW > 480.dp // Multi-window or tablet resizing mode

        if (useFrame) {
            val phoneWidth = 390.dp
            val phoneHeight = 844.dp

            Box(
                modifier = Modifier
                    .width(phoneWidth)
                    .height(phoneHeight)
                    .padding(12.dp)
                    .background(
                        color = if (isDark) Color(0xFF1F2C34) else Color(0xFFEFEAE2),
                        shape = RoundedCornerShape(40.dp)
                    )
                    .border(
                        width = 8.dp,
                        color = if (isDark) Color(0xFF2E3B43) else Color(0xFF111B21),
                        shape = RoundedCornerShape(40.dp)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(16.dp, shape = RoundedCornerShape(32.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        content()
                    }
                    SimulatedGestureBar(isDark = isDark)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
                SimulatedGestureBar(isDark = isDark)
            }
        }
    }
}

@Composable
fun SimulatedGestureBar(isDark: Boolean) {
    val barColor = if (isDark) Color(0xFF0B141A) else Color(0xFFEFEAE2)
    val pillColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(barColor)
            .height(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 4.dp)
                .background(pillColor, CircleShape)
        )
    }
}

fun getDisplayDateString(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val nowStr = sdf.format(Date())
    val msgStr = sdf.format(Date(timestamp))
    
    return if (msgStr == "20230502") {
        "May 2, 2023"
    } else if (msgStr == nowStr) {
        "Today"
    } else {
        val displaySdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        displaySdf.format(Date(timestamp))
    }
}

@Composable
fun DateHeaderPill(text: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = if (isDark) Color(0xFF182229) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 0.5.dp
        ) {
            Text(
                text = text,
                color = if (isDark) Color(0xFF8696A0) else Color(0xFF54656F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun SystemGroupLogPill(content: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = if (isDark) Color(0xFF182229) else Color(0xFFFFE0B2).copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 0.5.dp
        ) {
            Text(
                text = content,
                color = if (isDark) Color(0xFF8696A0) else Color(0xFF54656F),
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun WhatsAppTopAppBar(
    isDark: Boolean,
    onToggleDark: () -> Unit,
    showCustomizerPanel: Boolean,
    onToggleCustomizer: () -> Unit,
    onClearChat: () -> Unit
) {
    val containerColor = if (isDark) Color(0xFF1F2C34) else Color(0xFFFFFFFF)
    val contentColor = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21)
    val subtitleColor = if (isDark) Color(0xFF8696A0) else Color(0xFF54656F)

    Surface(
        color = containerColor,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* Back navigation placeholder */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFF25D366), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WBI",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WABetaInfo",
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isDark) "WBI, You" else "tap here for group info",
                    color = subtitleColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = { /* Video Call */ }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { /* Call */ }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleDark,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFFD54F) else contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleCustomizer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = if (showCustomizerPanel) Color(0xFF25D366) else contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WhatsAppInputArea(
    text: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean,
    onSendText: () -> Unit,
    onMicClick: () -> Unit,
    onAddClick: () -> Unit,
    onCameraClick: () -> Unit,
    isRecording: Boolean,
    recordingDuration: Long,
    onCancelRecord: () -> Unit,
    onSaveRecord: () -> Unit
) {
    val containerBg = if (isDark) Color(0xFF2A3942) else Color(0xFFFFFFFF)
    val contentColor = if (isDark) Color(0xFF8696A0) else Color(0xFF54656F)
    val greenButtonBg = Color(0xFF00A884)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRecording) {
            Surface(
                color = containerBg,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Smiley */ }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.SentimentSatisfied,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }

                    IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }

                    androidx.compose.foundation.text.BasicTextField(
                        value = text,
                        onValueChange = onValueChange,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text(
                                    text = "Message",
                                    color = contentColor,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    IconButton(onClick = onCameraClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            FloatingActionButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendText()
                    } else {
                        onMicClick()
                    }
                },
                containerColor = greenButtonBg,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (text.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                    contentDescription = null
                )
            }
        } else {
            Surface(
                color = containerBg,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingMicIcon()
                        Spacer(modifier = Modifier.width(8.dp))
                        val durationSecs = recordingDuration / 1000
                        val formattedDuration = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60)
                        Text(
                            text = "Recording ($formattedDuration)...",
                            color = Color(0xFFBA1A1A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = onCancelRecord
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            FloatingActionButton(
                onClick = onSaveRecord,
                containerColor = greenButtonBg,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WhatsAppMessageBubble(
    message: Message,
    isMe: Boolean,
    isDark: Boolean,
    isPlaying: Boolean,
    voiceProgress: Float,
    onVoicePlayClick: () -> Unit,
    onPhotoTap: (String) -> Unit,
    onVideoTap: (String) -> Unit,
    onDeleteRequest: () -> Unit
) {
    val dateString = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    val bubbleColor = if (isMe) {
        if (isDark) Color(0xFF005C4B) else Color(0xFFD9FDD3)
    } else {
        if (isDark) Color(0xFF202C33) else Color(0xFFFFFFFF)
    }

    val contentColor = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21)
    val metaColor = if (isDark) Color(0x99E9EDEF) else Color(0x80111B21)

    var showDeleteOption by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isMe) 12.dp else 2.dp,
                    bottomEnd = if (isMe) 2.dp else 12.dp
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.8.dp),
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            if (message.messageType == "PHOTO") {
                                message.mediaUri?.let { onPhotoTap(it) }
                            } else if (message.messageType == "VIDEO") {
                                message.mediaUri?.let { onVideoTap(it) }
                            }
                        },
                        onLongClick = { showDeleteOption = true }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    when (message.messageType) {
                        "TEXT" -> {
                            Text(
                                    text = message.content,
                                    color = contentColor,
                                    fontSize = 15.sp,
                                    lineHeight = 19.sp
                            )
                        }
                        "VOICE" -> {
                            VoiceMessageBubble(
                                isPlaying = isPlaying,
                                progress = voiceProgress,
                                durationMs = message.durationMs,
                                contentColor = contentColor,
                                onPlayClick = onVoicePlayClick
                            )
                        }
                        "PHOTO" -> {
                            Column {
                                message.mediaUri?.let { path ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = path,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .size(36.dp)
                                                .align(Alignment.Center),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "VIDEO" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateString,
                            color = metaColor,
                            fontSize = 9.5.sp
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = Color(0xFF53BDEB),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showDeleteOption) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDeleteRequest()
                            showDeleteOption = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showDeleteOption = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                    ) {
                        Text("Cancel", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
