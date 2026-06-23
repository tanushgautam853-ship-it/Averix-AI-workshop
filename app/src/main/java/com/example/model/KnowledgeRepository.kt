package com.example.model

import kotlinx.coroutines.flow.Flow

class KnowledgeRepository(private val dao: UserKnowledgeDao) {
    val allKnowledge: Flow<List<UserKnowledge>> = dao.getAllKnowledge()
    val knowledgeCount: Flow<Int> = dao.getKnowledgeCount()

    suspend fun insertKnowledge(question: String, answer: String) {
        dao.insertKnowledge(UserKnowledge(question = question, answer = answer))
    }
    
    suspend fun deleteKnowledge(id: Int) {
        dao.deleteKnowledge(id)
    }
    
    suspend fun clearAllKnowledge() {
        dao.clearAllKnowledge()
    }
}
