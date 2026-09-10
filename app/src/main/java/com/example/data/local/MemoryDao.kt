package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PersonalMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM personal_memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<PersonalMemory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: PersonalMemory): Long

    @Delete
    suspend fun deleteMemory(memory: PersonalMemory)

    @Query("DELETE FROM personal_memories")
    suspend fun clearAllMemories()
}
