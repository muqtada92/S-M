package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<Message>> = chatDao.getAllMessages()
    val preferences: Flow<ChatPreferences?> = chatDao.getPreferencesFlow()

    suspend fun insertMessage(message: Message) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessage(id)
    }

    suspend fun clearChat() {
        chatDao.clearChat()
    }

    suspend fun savePreferences(preferences: ChatPreferences) {
        chatDao.savePreferences(preferences)
    }
}
