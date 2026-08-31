package com.aurastudio.editor.language

import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.java.JavaLanguage

/**
 * Editor language registry (acs `editor/language` + `IDEEditor.setupLanguage`). acs binds
 * tree-sitter languages per extension; we keep a lightweight mapping so Java highlights without
 * requiring full language servers.
 */
internal fun editorLanguageFor(name: String): Language =
    if (name.endsWith(".java")) JavaLanguage() else EmptyLanguage()