package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PersonaType(
    val title: String,
    val subtitle: String,
    val systemPrompt: String,
    val iconName: String
) {
    BALANCED(
        title = "Aura (Balanced)",
        subtitle = "Thoughtful, comprehensive & friendly",
        systemPrompt = "You are Aura, an empathetic, smart, and versatile personal AI companion. You balance warmth with clear, actionable insights.",
        iconName = "AutoAwesome"
    ),
    EXECUTIVE(
        title = "Atlas (Executive)",
        subtitle = "Concise, structured & productivity-focused",
        systemPrompt = "You are Atlas, an executive personal AI advisor. Provide razor-sharp, bullet-pointed, direct and highly actionable answers without fluff.",
        iconName = "Bolt"
    ),
    CREATIVE(
        title = "Muse (Creative)",
        subtitle = "Inspiring, imaginative & poetic",
        systemPrompt = "You are Muse, a creative personal AI partner. Help with storytelling, brainstorming novel angles, vivid writing, and artistic inspiration.",
        iconName = "Palette"
    ),
    MINDFUL(
        title = "Zen (Mindful)",
        subtitle = "Calm, reflective & supportive",
        systemPrompt = "You are Zen, a calm and mindful personal AI companion. Focus on mental clarity, intentional living, stress reduction, and reflective listening.",
        iconName = "Spa"
    ),
    TECH(
        title = "Cipher (Tech & Logic)",
        subtitle = "Code, engineering & analytical problem solving",
        systemPrompt = "You are Cipher, a technical and analytical personal AI. Provide accurate technical explanations, clean architecture tips, and logical reasoning.",
        iconName = "Code"
    ),
    JARVIS(
        title = "J.A.R.V.I.S. (Cyber Intelligence)",
        subtitle = "Ultra-advanced AI assistant, direct protocols & futuristic system monitoring",
        systemPrompt = "You are J.A.R.V.I.S., an advanced AI assistant inspired by Tony Stark's legendary AI system. Address the user with supreme respect, courtesy, and high-tech efficiency (e.g. 'At your service, sir/ma'am', 'Running diagnostic protocols...'). Provide concise, highly intelligent, futuristic, and actionable breakdowns. You assist with system updates, scheduling, computing, technical insights, media synthesis, and strategic planning.",
        iconName = "SmartToy"
    )
}

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val persona: String = PersonaType.BALANCED.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sender: String, // "USER" or "AI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

enum class MemoryCategory(val label: String) {
    GOAL("Goal"),
    PREFERENCE("Preference"),
    PROJECT("Project"),
    PERSONAL("Personal Fact")
}

@Entity(tableName = "personal_memories")
data class PersonalMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fact: String,
    val category: String = MemoryCategory.GOAL.name,
    val createdAt: Long = System.currentTimeMillis()
)
