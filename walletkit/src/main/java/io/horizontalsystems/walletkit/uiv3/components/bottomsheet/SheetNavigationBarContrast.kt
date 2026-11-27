package io.horizontalsystems.walletkit.uiv3.components.bottomsheet

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Turns off the system contrast scrim behind the navigation bar of the sheet's window.
 *
 * A ModalBottomSheet opens its own window, so the activity's setting does not reach it, and
 * the sheet paints its own background under the navigation bar. Call it from the sheet's
 * content.
 */
@Composable
fun DisableSheetNavigationBarContrast() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

    val view = LocalView.current
    SideEffect {
        val provider = view as? DialogWindowProvider ?: view.parent as? DialogWindowProvider
        provider?.window?.isNavigationBarContrastEnforced = false
    }
}
