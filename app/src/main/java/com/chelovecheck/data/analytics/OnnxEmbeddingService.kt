package com.chelovecheck.data.analytics

import com.chelovecheck.domain.repository.CategoryEmbeddingService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class OnnxEmbeddingService @Inject constructor(
    private val provider: OnnxSentenceEmbedderProvider,
) : CategoryEmbeddingService {
    override suspend fun embed(text: String): FloatArray {
        return withContext(Dispatchers.Default) {
            provider.stage1.embed(text)
        }
    }
}
