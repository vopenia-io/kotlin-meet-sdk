package io.vopenia.sdk.permissions

import androidx.fragment.app.FragmentActivity
import io.vopenia.livekit.PermissionsActivityController as PAC

object PermissionsActivityController {
    fun setActivity(activity: FragmentActivity) {
        PAC.setActivity(activity)
    }
}
