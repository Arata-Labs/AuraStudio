package com.aurastudio.data.bootstrap

import androidx.compose.runtime.Immutable

@Immutable
data class BootstrapState(
    val running: Boolean = false,
    val done: Boolean = false,
    val phase: String = "",
    val extractedFiles: Int = 0,
    val log: List<String> = emptyList(),
    val errorTitle: String? = null,
    val errorMessage: String? = null,
) {
    val hasError: Boolean get() = errorMessage != null
}