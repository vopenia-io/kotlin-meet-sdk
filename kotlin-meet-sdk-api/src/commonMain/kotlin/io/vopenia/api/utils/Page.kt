package io.vopenia.api.utils

import kotlinx.serialization.Serializable

@Serializable
data class Page<T>(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T>
)
