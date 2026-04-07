package com.chelovecheck.data.analytics

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.chelovecheck.domain.logging.AppLogger
import java.nio.LongBuffer
import kotlin.math.exp

class OnnxNliClassifier(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val tokenizer: UnigramTokenizer,
    private val logger: AppLogger,
    private val tag: String,
    private val entailmentIndex: Int = 2,
) {
    private val inputNames = session.inputNames.toSet()
    private val hasTokenType = inputNames.contains("token_type_ids")

    fun entailmentScore(premise: String, hypothesis: String): Float {
        return runCatching {
            val encoding = tokenizer.encodePair(premise, hypothesis)
            val inputShape = longArrayOf(1L, encoding.inputIds.size.toLong())
            val inputIds = OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.inputIds), inputShape)
            val attentionMask = OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.attentionMask), inputShape)
            val tokenTypeIds = if (hasTokenType && encoding.tokenTypeIds != null) {
                OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.tokenTypeIds), inputShape)
            } else {
                null
            }
            inputIds.use { idsTensor ->
                attentionMask.use { maskTensor ->
                    val inputs = mutableMapOf(
                        "input_ids" to idsTensor,
                        "attention_mask" to maskTensor,
                    )
                    tokenTypeIds?.use { typesTensor ->
                        inputs["token_type_ids"] = typesTensor
                    }
                    session.run(inputs).use { result ->
                        val output = result[0].value
                        val logits = when (output) {
                            is Array<*> -> output.firstOrNull() as? FloatArray ?: return 0f
                            is FloatArray -> output
                            else -> return 0f
                        }
                        val probs = softmax(logits)
                        return probs.getOrElse(entailmentIndex) { 0f }
                    }
                }
            }
            0f
        }.onFailure { e ->
            logger.error(tag, "nli failed: ${e.message}", e)
        }.getOrDefault(0f)
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = FloatArray(logits.size)
        var sum = 0.0
        for (i in logits.indices) {
            val v = exp((logits[i] - max).toDouble())
            exps[i] = v.toFloat()
            sum += v
        }
        if (sum == 0.0) return exps
        for (i in exps.indices) {
            exps[i] = (exps[i] / sum).toFloat()
        }
        return exps
    }
}
