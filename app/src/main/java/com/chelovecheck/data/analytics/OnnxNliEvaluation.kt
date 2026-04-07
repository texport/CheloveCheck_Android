package com.chelovecheck.data.analytics

/**
 * Placeholder for optional **second-stage NLI** disambiguation ([OnnxNliProvider]) on conflicting
 * lexical vs embedding pairs. Not wired into [com.chelovecheck.data.analytics.pipeline.CategoryPredictionPipeline]
 * by default to preserve latency; enable only after offline evaluation on a golden set.
 */
object OnnxNliEvaluation {
    const val NOTE =
        "Evaluate NLI on pairs where mergeLexicalAndEmbedding keeps uncertainty; budget <5ms per line on mid devices."
}
