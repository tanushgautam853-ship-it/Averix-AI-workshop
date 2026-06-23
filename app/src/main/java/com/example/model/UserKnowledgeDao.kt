package com.example.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserKnowledgeDao {
    @Query("SELECT * FROM user_knowledge ORDER BY timestamp ASC")
    fun getAllKnowledge(): Flow<List<UserKnowledge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: UserKnowledge)

    @Query("DELETE FROM user_knowledge WHERE id = :id")
    suspend fun deleteKnowledge(id: Int)

    @Query("DELETE FROM user_knowledge")
    suspend fun clearAllKnowledge()

    @Query("SELECT COUNT(*) FROM user_knowledge")
    fun getKnowledgeCount(): Flow<Int>
}
