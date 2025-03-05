package dev.balakumar.promptforge.models

data class RelatedFile(
    val path: String,
    val content: String,
    val isDecompiled: Boolean = false,
    val isImplementation: Boolean = false
)