package com.chelovecheck.data.analytics

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.AnalyticsLoadStage
import com.chelovecheck.domain.repository.AnalyticsProgressReporter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnnxSentenceEmbedderProvider @Inject constructor(
    private val modelProvider: OnnxModelProvider,
    private val tokenizerFactory: WordPieceTokenizerFactory,
    private val logger: AppLogger,
    private val progressReporter: AnalyticsProgressReporter,
) {
    private val env = OrtEnvironment.getEnvironment()

    val stage1: OnnxSentenceEmbedder by lazy {
        logger.debug(
            TAG,
            "analytics embedding model revision=${AnalyticsEmbeddingModelInfo.LOGICAL_MODEL_REVISION} " +
                "id=${AnalyticsEmbeddingModelInfo.HUGGINGFACE_MODEL_ID}",
        )
        createEmbedderWithFallback(
            primaryAsset = MODEL_INT8,
            primaryOptLevel = OrtSession.SessionOptions.OptLevel.NO_OPT,
            fallbackAsset = MODEL_FP16,
        )
    }

    val stage2: OnnxSentenceEmbedder by lazy {
        try {
            createEmbedder(MODEL_FP16, OrtSession.SessionOptions.OptLevel.NO_OPT)
        } catch (e: OrtException) {
            logger.error(TAG, "Stage2 model load failed, using stage1 embedder: ${e.message}", e)
            stage1
        }
    }

    private fun createEmbedder(assetPath: String, optLevel: OrtSession.SessionOptions.OptLevel): OnnxSentenceEmbedder {
        val file = modelProvider.getModelFile(assetPath)
        progressReporter.report(AnalyticsLoadStage.LOADING_MODEL)
        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
            setInterOpNumThreads(1)
            setOptimizationLevel(optLevel)
        }
        val session = env.createSession(file.absolutePath, sessionOptions)
        logger.debug(
            TAG,
            "loaded model=$assetPath size=${file.length()} opt=${optLevel.name} inputs=${session.inputNames} outputs=${session.outputNames}",
        )
        return OnnxSentenceEmbedder(env, session, tokenizerFactory.get(), logger, "$TAG:$assetPath")
    }

    private fun createEmbedderWithFallback(
        primaryAsset: String,
        primaryOptLevel: OrtSession.SessionOptions.OptLevel,
        fallbackAsset: String,
    ): OnnxSentenceEmbedder {
        return try {
            createEmbedder(primaryAsset, primaryOptLevel)
        } catch (e: OrtException) {
            logger.error(TAG, "Stage1 model load failed, falling back to fp16: ${e.message}", e)
            createEmbedder(fallbackAsset, OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
    }

    companion object {
        private const val MODEL_INT8 = AnalyticsEmbeddingModelInfo.PRIMARY_ONNX
        private const val MODEL_FP16 = AnalyticsEmbeddingModelInfo.FALLBACK_ONNX
        private const val TAG = "OnnxEmbedder"
    }
}
