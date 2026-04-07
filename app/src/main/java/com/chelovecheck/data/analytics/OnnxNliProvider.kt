package com.chelovecheck.data.analytics

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.chelovecheck.domain.logging.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnnxNliProvider @Inject constructor(
    private val modelProvider: OnnxModelProvider,
    private val tokenizerFactory: NliTokenizerFactory,
    private val logger: AppLogger,
) {
    private val env = OrtEnvironment.getEnvironment()

    val classifier: OnnxNliClassifier? by lazy {
        runCatching {
            val model = modelProvider.getModelFile(MODEL_ASSET)
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setInterOpNumThreads(1)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT)
            }
            val session = env.createSession(model.absolutePath, options)
            logger.debug(TAG, "loaded model=$MODEL_ASSET size=${model.length()} inputs=${session.inputNames} outputs=${session.outputNames}")
            OnnxNliClassifier(env, session, tokenizerFactory.get(), logger, TAG)
        }.onFailure { e ->
            logger.error(TAG, "nli init failed: ${e.message}", e)
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "OnnxNli"
        private const val MODEL_ASSET = "ml/nli/onnx/model_int8.onnx"
    }
}
