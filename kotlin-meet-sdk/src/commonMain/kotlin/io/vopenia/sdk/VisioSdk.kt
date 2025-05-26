package io.vopenia.sdk

import io.vopenia.sdk.utils.AuthenticationInformation

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