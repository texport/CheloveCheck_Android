package com.chelovecheck.data.analytics

/**
 * Audit metadata for the on-device sentence embedding model used in analytics (rollup COICOP index).
 *
 * **Model:** Hugging Face `sentence-transformers/distiluse-base-multilingual-cased-v2`
 * (ONNX assets loaded via [OnnxSentenceEmbedderProvider]).
 *
 * **Languages:** trained for many languages including **Russian, English, and Kazakh**; suitable for
 * **code-mixed** product lines (RU + EN + KK in one string), which is typical on Kazakhstan retail receipts.
 *
 * **Evaluation:** acceptance for a replacement model should include accuracy on **mixed trilingual** item
 * names (not only monolingual), plus APK size and inference latency on target devices.
 */
object AnalyticsEmbeddingModelInfo {
    const val HUGGINGFACE_MODEL_ID = "sentence-transformers/distiluse-base-multilingual-cased-v2"
    const val ONNX_ASSET_DIR = "ml/distiluse-base-multilingual-cased-v2/onnx"
    const val PRIMARY_ONNX = "$ONNX_ASSET_DIR/model_int8.onnx"
    const val FALLBACK_ONNX = "$ONNX_ASSET_DIR/model_fp16.onnx"

    /** Bump when ONNX weights or tokenizer change; used in logs and coordination with cache versioning. */
    const val LOGICAL_MODEL_REVISION = 12
}
