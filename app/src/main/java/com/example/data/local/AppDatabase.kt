package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MemoryCategory
import com.example.data.model.PersonalMemory
import com.example.data.model.PersonaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ChatSession::class, ChatMessage::class, PersonalMemory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_ai_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.chatDao(), database.memoryDao())
                    }
                }
            }

            private suspend fun populateInitialData(chatDao: ChatDao, memoryDao: MemoryDao) {
                // Initial session
                val initialSessionId = chatDao.insertSession(
                    ChatSession(
                        title = "Welcome & Getting Started",
                        persona = PersonaType.BALANCED.name,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Initial welcome messages
                chatDao.insertMessage(
                    ChatMessage(
                        sessionId = initialSessionId,
                        sender = "AI",
                        content = "Hello! I am your Personal AI companion.\n\nI can help you brainstorm ideas, summarize thoughts, structure goals, or provide guidance tailored specifically to you.\n\nCheck out the **AI Memory** tab to teach me about your goals and preferences so I can assist you better!",
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Initial starter memories
                memoryDao.insertMemory(
                    PersonalMemory(
                        fact = "Goal: Build high-impact Android applications and master modern Kotlin",
                        category = MemoryCategory.GOAL.name
                    )
                )
                memoryDao.insertMemory(
                    PersonalMemory(
                        fact = "Preference: Enjoys clean, structured, and insightful explanations",
                        category = MemoryCategory.PREFERENCE.name
                    )
                )
            }
        }
    }
}
