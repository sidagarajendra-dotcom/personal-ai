package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.MemoryDao
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.PersonalMemory
import com.example.data.model.PersonaType
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow

class PersonalAiRepository(
    private val chatDao: ChatDao,
    private val memoryDao: MemoryDao,
    private val geminiService: GeminiService
) {
    // Sessions
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getSession(sessionId: Long): Flow<ChatSession?> = chatDao.getSessionById(sessionId)

    suspend fun getLatestSession(): ChatSession? = chatDao.getLatestSession()

    suspend fun createNewSession(title: String, persona: PersonaType): Long {
        val session = ChatSession(
            title = title.ifBlank { "New Conversation" },
            persona = persona.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val sessionId = chatDao.insertSession(session)
        // Add greeting
        chatDao.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                sender = "AI",
                content = "Started new session with **${persona.title}**. What would you like to explore today?",
                timestamp = System.currentTimeMillis()
            )
        )
        return sessionId
    }

    suspend fun updateSessionTitle(session: ChatSession, newTitle: String) {
        chatDao.updateSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateSessionPersona(session: ChatSession, persona: PersonaType) {
        chatDao.updateSession(session.copy(persona = persona.name, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSession(session: ChatSession) {
        chatDao.deleteMessagesForSession(session.id)
        chatDao.deleteSession(session)
    }

    suspend fun clearAllHistory() {
        chatDao.clearAllMessages()
        chatDao.clearAllSessions()
    }

    // Messages
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun sendMessage(
        sessionId: Long,
        userText: String,
        session: ChatSession,
        currentMessages: List<ChatMessage>,
        memories: List<PersonalMemory>,
        customApiKey: String? = null,
        temperature: Float = 0.7f
    ) {
        val now = System.currentTimeMillis()

        // 1. Insert user message
        val userMsg = ChatMessage(
            sessionId = sessionId,
            sender = "USER",
            content = userText.trim(),
            timestamp = now
        )
        chatDao.insertMessage(userMsg)

        // Update session timestamp & auto-update title if it's default
        val newTitle = if (session.title == "New Conversation" || session.title == "Welcome & Getting Started") {
            userText.take(30).trim() + if (userText.length > 30) "..." else ""
        } else {
            session.title
        }
        chatDao.updateSession(session.copy(title = newTitle, updatedAt = now))

        // 2. Call Gemini
        val persona = try {
            PersonaType.valueOf(session.persona)
        } catch (e: Exception) {
            PersonaType.BALANCED
        }

        val aiResponseText = geminiService.generateAiResponse(
            prompt = userText,
            history = currentMessages + userMsg,
            persona = persona,
            memories = memories,
            customApiKey = customApiKey,
            temperature = temperature
        )

        // 3. Insert AI response
        val aiMsg = ChatMessage(
            sessionId = sessionId,
            sender = "AI",
            content = aiResponseText,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(aiMsg)
        chatDao.updateSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    // Memories
    val allMemories: Flow<List<PersonalMemory>> = memoryDao.getAllMemories()

    suspend fun addMemory(fact: String, category: String) {
        memoryDao.insertMemory(
            PersonalMemory(
                fact = fact.trim(),
                category = category,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMemory(memory: PersonalMemory) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun clearMemories() {
        memoryDao.clearAllMemories()
    }
}
