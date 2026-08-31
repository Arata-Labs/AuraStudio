package com.aurastudio.filetree.models

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

/* acs `FileExtension` port, restyled: every extension style is a distinct tinted glyph (full icons
 * rather than letter badges) so the tree reads cleanly by file type. */

private const val ANDROID_GREEN = 0xFF3DDC84
private const val KOTLIN_PURPLE = 0xFFCD7EE0
private const val JAVA_AMBER = 0xFFE08A3C
private const val XML_TEAL = 0xFF61AFEF
private const val JSON_GOLD = 0xFFC9A227
private const val TOML_BROWN = 0xFFB0703A
private const val YAML_RED = 0xFFCB4B34
private const val R8_CYAN = 0xFF56B6C2
private const val PROPS_GRAY = 0xFF8B8D96
private const val MD_BLUE = 0xFF6C9BD1
private const val GIT_ORANGE = 0xFFDE6E43
private const val GRADLE_GREEN = 0xFF6BA84F
private const val JS_YELLOW = 0xFFE8C547
private const val SHELL_GREEN = 0xFF7BC96F
private const val WEB_CYAN = 0xFF5AC8E0

private data class FileIconSpec(val glyph: ImageVector, val accent: Color)

private fun fileIconSpec(name: String, fallback: Color): FileIconSpec {
    val n = name
    val ext = n.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when {
        n == "AndroidManifest.xml" ->
            FileIconSpec(Icons.Filled.FactCheck, Color(ANDROID_GREEN))
        n in setOf(".gitignore", ".gitattributes", ".gitmodules", ".gitkeep") ->
            FileIconSpec(Icons.Filled.AccountTree, Color(GIT_ORANGE))
        n == ".editorconfig" -> FileIconSpec(Icons.Filled.ManageSearch, Color(PROPS_GRAY))
        n.endsWith(".pro") -> FileIconSpec(Icons.Filled.Build, Color(R8_CYAN))
        n.endsWith(".aidl") -> FileIconSpec(Icons.Filled.Api, Color(ANDROID_GREEN))
        ext == "java" -> FileIconSpec(Icons.Filled.Code, Color(JAVA_AMBER))
        ext in setOf("kt", "kts") -> FileIconSpec(Icons.Filled.IntegrationInstructions, Color(KOTLIN_PURPLE))
        ext == "gradle" -> FileIconSpec(Icons.Filled.Build, Color(GRADLE_GREEN))
        ext == "xml" -> FileIconSpec(Icons.Filled.Web, Color(XML_TEAL))
        ext == "json" -> FileIconSpec(Icons.Filled.DataObject, Color(JSON_GOLD))
        ext == "toml" -> FileIconSpec(Icons.Filled.Schema, Color(TOML_BROWN))
        ext in setOf("yaml", "yml") -> FileIconSpec(Icons.Filled.Notes, Color(YAML_RED))
        ext == "properties" -> FileIconSpec(Icons.Filled.Tune, Color(PROPS_GRAY))
        ext in setOf("md", "markdown") -> FileIconSpec(Icons.Filled.MenuBook, Color(MD_BLUE))
        ext == "sh" || ext == "zsh" || ext == "bash" -> FileIconSpec(Icons.Filled.Terminal, Color(SHELL_GREEN))
        ext == "py" -> FileIconSpec(Icons.Filled.PlayArrow, Color(SHELL_GREEN))
        ext == "js" || ext == "mjs" || ext == "cjs" -> FileIconSpec(Icons.Filled.Javascript, Color(JS_YELLOW))
        ext == "ts" || ext == "tsx" -> FileIconSpec(Icons.Filled.Terminal, Color(MD_BLUE))
        ext == "html" || ext == "htm" -> FileIconSpec(Icons.Filled.Html, Color(WEB_CYAN))
        ext == "css" || ext == "scss" || ext == "less" -> FileIconSpec(Icons.Filled.Css, Color(JS_YELLOW))
        ext in setOf("c", "cc", "cpp", "h", "hh", "hpp") -> FileIconSpec(Icons.Filled.Memory, Color(XML_TEAL))
        ext in setOf("txt", "log") -> FileIconSpec(Icons.Filled.Description, fallback)
        ext in setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "bmp") ->
            FileIconSpec(Icons.Filled.Image, Color(0xFF5AC8E0))
        else -> FileIconSpec(Icons.AutoMirrored.Filled.InsertDriveFile, fallback)
    }
}

/** Distinct tinted glyph per extension. Size defaults to 17.dp. */
@Composable
internal fun FileIcon(name: String, modifier: Modifier = Modifier, size: Dp = 17.dp) {
    val spec = fileIconSpec(name, MaterialTheme.colorScheme.outline)
    Icon(
        spec.glyph,
        contentDescription = null,
        tint = spec.accent,
        modifier = modifier.size(size)
    )
}
