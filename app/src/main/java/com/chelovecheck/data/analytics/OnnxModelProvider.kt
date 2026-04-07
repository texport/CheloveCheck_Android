package com.chelovecheck.data.analytics

import android.content.Context
import com.chelovecheck.domain.logging.AppLogger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnnxModelProvider @Inject constructor(
    private val context: Context,
    private val logger: AppLogger,
) {
    private val cacheDir = File(context.filesDir, "onnx_models").apply { mkdirs() }

    fun getModelFile(assetPath: String): File {
        val target = File(cacheDir, assetPath.replace("/", "_"))
        if (target.exists()) {
            logger.debug(TAG, "model cached: $assetPath size=${target.length()}")
            return target
        }
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        logger.debug(TAG, "model copied: $assetPath size=${target.length()}")
        return target
    }

    companion object {
        private const val TAG = "OnnxModelProvider"
    }
}
