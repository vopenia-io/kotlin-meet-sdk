package io.vopenia.sdk

import io.vopenia.api.AuthenticationInformation

object VisioSdk {
    fun openSession(
        prefixHttp: String,
        enableHttpLog: Boolean,
        refreshAuthentication: suspend () -> AuthenticationInformation
    ) = Session(
        prefixHttp,
        enableHttpLog,
        refreshAuthentication
    )
}