package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

@Serializable
data class InviteEmails(
    val emails: List<String>
)
