package com.chelovecheck.data.analytics

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import com.chelovecheck.domain.logging.AppLogger
import java.nio.LongBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class OnnxSentenceEmbedder(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val tokenizer: WordPieceTokenizer,
    private val logger: AppLogger,
    private val tag: String,
) {
    private val inputNames = session.inputNames.toSet()
    private val hasTokenType = inputNames.contains("token_type_ids")
    private val loggedOnce = AtomicBoolean(false)

    fun embed(text: String): FloatArray {
        return try {
            val encoded = tokenizer.encode(text)
            if (loggedOnce.compareAndSet(false, true)) {
                logger.debug(
                    tag,
                    "embed init: inputNames=$inputNames hasTokenType=$hasTokenType maxLen=${encoded.inputIds.size}",
                )
            }
            val inputShape = longArrayOf(1L, encoded.inputIds.size.toLong())
            val inputIds = OnnxTensor.createTensor(env, LongBuffer.wrap(encoded.inputIds), inputShape)
            val attentionMask = OnnxTensor.createTensor(env, LongBuffer.wrap(encoded.attentionMask), inputShape)
            val tokenTypeIds = if (hasTokenType) {
                val tokenType = encoded.tokenTypeIds ?: LongArray(encoded.inputIds.size) { 0L }
                OnnxTensor.createTensor(env, LongBuffer.wrap(tokenType), inputShape)
            } else {
                null
            }

            inputIds.use { ids ->
                attentionMask.use { mask ->
                    tokenTypeIds?.use { tokenType ->
                        val inputs = mutableMapOf<String, OnnxTensor>(
                            "input_ids" to ids,
                            "attention_mask" to mask,
                        )
                        if (hasTokenType) {
                            inputs["token_type_ids"] = tokenType
                        }
                        session.run(inputs).use { result ->
                            val raw = result[0].value
                            val tokenEmbeddings = extractHiddenState(raw)
                            val pooled = meanPool(tokenEmbeddings, encoded.attentionMask)
                            return l2Normalize(pooled)
                        }
                    }
                    if (!hasTokenType) {
                        val inputs = mutableMapOf<String, OnnxTensor>(
                            "input_ids" to ids,
                            "attention_mask" to mask,
                        )
                        session.run(inputs).use { result ->
                            val raw = result[0].value
                            val tokenEmbeddings = extractHiddenState(raw)
                            val pooled = meanPool(tokenEmbeddings, encoded.attentionMask)
                            return l2Normalize(pooled)
                        }
                    }
                }
            }
            FloatArray(0)
        } catch (e: OrtException) {
            logger.error(tag, "embed failed: ${e.message}", e)
            FloatArray(0)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractHiddenState(value: Any): Array<FloatArray> {
        val batch = value as Array<*>
        return batch[0] as Array<FloatArray>
    }

    private fun meanPool(tokens: Array<FloatArray>, mask: LongArray): FloatArray {
        val hiddenSize = tokens.firstOrNull()?.size ?: return FloatArray(0)
        val sum = FloatArray(hiddenSize)
        var count = 0
        for (i in tokens.indices) {
            if (mask.getOrNull(i) != 1L) continue
            val vec = tokens[i]
            for (j in 0 until hiddenSize) {
                sum[j] += vec[j]
            }
            count++
        }
        if (count > 0) {
            for (j in sum.indices) {
                sum[j] /= count
            }
        }
        return sum
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) sum += v * v
        val norm = sqrt(sum)
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }
}
