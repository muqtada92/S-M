package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: String,          // "user_a" or "user_b"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT", // "TEXT", "VOICE", "PHOTO", "VIDEO"
    val mediaUri: String? = null,    // Local file path or content URI
    val durationMs: Long = 0L,       // Duration in ms for voice notes
    val messageUuid: String = ""     // Unique identifier for internet syncing
)

@Entity(tableName = "chat_preferences")
data class ChatPreferences(
    @PrimaryKey val id: String = "singleton",
    val sentColorHex: String = "#006A6A",       // Default sender bubble hex color (Teal)
    val receivedColorHex: String = "#FFFFFF",   // Default receiver bubble hex color (White)
    val backgroundType: String = "PRESET",      // "PRESET", "GALLERY", "SOLID"
    val presetId: String = "preset_slate",      // Default beautiful preset (Minimal Slate Light)
    val customBackgroundUri: String? = null,    // User dynamic selected image path
    val solidColorHex: String = "#FEF7FF",      // Solid backdrop color hex
    val syncRoomCode: String = "",              // Pairing/Syncing room key ("" is offline)
    val selfUserId: String = "user_a"           // Identification of this device: "user_a" or "user_b"
)
