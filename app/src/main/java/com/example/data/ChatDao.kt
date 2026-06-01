package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages")
    suspend fun clearChat()

    @Query("SELECT * FROM chat_preferences WHERE id = 'singleton' LIMIT 1")
    fun getPreferencesFlow(): Flow<ChatPreferences?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(preferences: ChatPreferences)
}
