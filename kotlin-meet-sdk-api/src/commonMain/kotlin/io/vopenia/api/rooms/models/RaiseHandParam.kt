package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

/**
 * Body for the backend `toggle-hand` endpoint. Mirrors Meet Web
 * `updateRaiseHand.ts`: the body is `{ raised }` only, while the LiveKit token
 * travels in the `Authorization: Bearer <token>` header (read by the backend's
 * `LiveKitTokenAuthentication` — upstream switched body->header in 5d7a54e8 on
 * 2026-04-08).
 *
 * The deployed backend revokes `canUpdateOwnMetadata`, so clients can no longer
 * write the `handRaisedAt` attribute directly and must go through this endpoint;
 * the server stamps the timestamp and writes the attribute via the server SDK.
 */
@Serializable
data class RaiseHandParam(
    val raised: Boolean
)
