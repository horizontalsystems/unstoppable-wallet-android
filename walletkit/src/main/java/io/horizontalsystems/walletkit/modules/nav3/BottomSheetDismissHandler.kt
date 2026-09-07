package io.horizontalsystems.walletkit.modules.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/** Where a sheet's content registers its [BottomSheetDismissHandler]; owned by the sheet scene. */
internal class BottomSheetDismissRegistry {
    var handler: (() -> Unit)? = null
}

internal val LocalBottomSheetDismissRegistry =
    staticCompositionLocalOf<BottomSheetDismissRegistry?> { null }

/**
 * Replaces the default pop when the user dismisses the enclosing nav3 bottom sheet by swiping it
 * down, tapping the scrim or pressing back. [onDismiss] decides what happens next, typically some
 * cleanup followed by popping the entry itself. Closing through the sheet's own buttons is not
 * affected. Does nothing outside a bottom-sheet scene.
 */
@Composable
fun BottomSheetDismissHandler(onDismiss: () -> Unit) {
    val registry = LocalBottomSheetDismissRegistry.current ?: return
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(registry) {
        val handler = { currentOnDismiss() }
        registry.handler = handler
        onDispose {
            if (registry.handler === handler) registry.handler = null
        }
    }
}
