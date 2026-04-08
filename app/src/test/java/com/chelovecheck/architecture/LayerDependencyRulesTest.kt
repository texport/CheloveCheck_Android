package com.chelovecheck.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerDependencyRulesTest {
    @Test
    fun dataLayer_doesNotDependOnPresentationLayer() {
        val dataRoot = projectPath("app/src/main/java/com/chelovecheck/data")
        val offenders = kotlinFiles(dataRoot).filter { path ->
            path.readText().contains("import com.chelovecheck.presentation.")
        }
        assertTrue(
            "Data layer must not import presentation. Offenders:\n${offenders.joinToString("\n")}",
            offenders.isEmpty(),
        )
    }

    @Test
    fun presentationLayer_doesNotImportDataLayer() {
        val presentationRoot = projectPath("app/src/main/java/com/chelovecheck/presentation")
        val offenders = kotlinFiles(presentationRoot).filter { path ->
            path.readText().lineSequence().any { line ->
                val t = line.trimStart()
                t.startsWith("import com.chelovecheck.data.")
            }
        }
        assertTrue(
            "Presentation must not import data layer (use domain ports). Offenders:\n${offenders.joinToString("\n")}",
            offenders.isEmpty(),
        )
    }

    @Test
    fun domainLayer_hasNoAndroidOrAndroidxImports() {
        val domainRoot = projectPath("app/src/main/java/com/chelovecheck/domain")
        val offenders = kotlinFiles(domainRoot).filter { path ->
            val text = path.readText()
            text.contains("import android.") || text.contains("import androidx.")
        }
        assertTrue(
            "Domain layer must stay platform-agnostic. Offenders:\n${offenders.joinToString("\n")}",
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
}
