package com.chelovecheck.domain.repository

interface CategoryEmbeddingService {
    suspend fun embed(text: String): FloatArray
}
