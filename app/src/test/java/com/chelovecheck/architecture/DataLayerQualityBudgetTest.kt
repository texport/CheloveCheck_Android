package com.chelovecheck.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readLines
import org.junit.Assert.assertTrue
import org.junit.Test

class DataLayerQualityBudgetTest {
    @Test
    fun kotlinFiles_stayWithinClassLineBudget() {
        val dataRoot = projectPath("app/src/main/java/com/chelovecheck/data")
        val offenders = kotlinFiles(dataRoot).mapNotNull { path ->
            val lineCount = path.readLines().size
            if (lineCount > MAX_CLASS_LINES) "${path.name}: $lineCount" else null
        }
        assertTrue(
            "Class line budget exceeded (max=$MAX_CLASS_LINES):\n${offenders.joinToString("\n")}",
            offenders.isEmpty(),
        )
    }

    @Test
    fun kotlinFiles_stayWithinFunctionBodyBudget() {
        val dataRoot = projectPath("app/src/main/java/com/chelovecheck/data")
        val offenders = mutableListOf<String>()
        kotlinFiles(dataRoot).forEach { path ->
            val lines = path.readLines()
            var braceDepth = 0
            var functionName: String? = null
            var functionStart = -1
            var functionBraceDepth = 0
            lines.forEachIndexed { index, raw ->
                val line = raw.trim()
                if (functionName == null) {
                    val fnMatch = FUNCTION_SIGNATURE.find(line)
                    if (fnMatch != null) {
                        functionName = fnMatch.groupValues[1]
                        functionStart = index + 1
                        functionBraceDepth = braceDepth
                    }
                }
                braceDepth += raw.count { it == '{' }
                braceDepth -= raw.count { it == '}' }
                if (functionName != null && braceDepth <= functionBraceDepth) {
                    val length = index + 1 - functionStart + 1
                    if (length > MAX_FUNCTION_LINES) {
                        offenders += "${path.name}: ${functionName}() = $length lines"
                    }
                    functionName = null
                    functionStart = -1
                    functionBraceDepth = 0
                }
            }
        }
        assertTrue(
            "Function body budget exceeded (max=$MAX_FUNCTION_LINES):\n${offenders.joinToString("\n")}",
            offenders.isEmpty(),
        )
    }

    private fun projectPath(relative: String): Path {
        val cwd = Paths.get("").toAbsolutePath()
        val asIs = cwd.resolve(relative)
        if (Files.exists(asIs)) return asIs
        val oneUp = cwd.parent?.resolve(relative)
        if (oneUp != null && Files.exists(oneUp)) return oneUp
        error("Cannot locate project path for '$relative' from '$cwd'")
    }

    private fun kotlinFiles(root: Path): List<Path> {
        return Files.walk(root)
            .filter { it.isRegularFile() && it.extension == "kt" }
            .toList()
    }

    private companion object {
        private const val MAX_CLASS_LINES = 500
        private const val MAX_FUNCTION_LINES = 140
        private val FUNCTION_SIGNATURE = Regex("""fun\s+([A-Za-z0-9_]+)\s*\(""")
    }
}
