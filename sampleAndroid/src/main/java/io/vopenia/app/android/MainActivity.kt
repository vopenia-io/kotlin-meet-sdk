package io.vopenia.app.android

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import io.vopenia.livekit.PermissionsActivityController
import io.vopenia.app.App
import io.vopenia.app.AppBackPressProvider

class MainActivity : FragmentActivity() {
    private val onBackPressProvider = AppBackPressProvider()
    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (onBackPressProvider.onBackPress()) {
                return
            }

            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PermissionsActivityController.setActivity(this)
        onBackPressedDispatcher.addCallback(this, backPressCallback)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(
            ComposeView(this).apply {
                setContent {
                    Box {
                        App(
                            isDarkTheme = isSystemInDarkTheme(),
                            onBackPressed = onBackPressProvider,
                        )
                    }
                }
            }
        )
    }
}
