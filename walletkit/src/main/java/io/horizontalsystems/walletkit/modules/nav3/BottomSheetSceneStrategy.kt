package io.horizontalsystems.walletkit.modules.nav3

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.modules.nav3.BottomSheetSceneStrategy.Companion.bottomSheet

/** An [OverlayScene] that renders an [entry] within a [ModalBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val modalBottomSheetProperties: ModalBottomSheetProperties,
    private val skipPartiallyExpanded: Boolean,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        // ModalBottomSheet opens its own window, composited above the activity window — and so
        // above the PinUnlock overlay, which would leave the sheet interactable over the keypad.
        // The lock also engages asynchronously (PinComponent collects background state on a
        // Default-dispatcher coroutine), so a sheet can slip onto the stack in the same frame the
        // app locks. Keep the entry on the stack but don't compose the sheet while locked: the
        // window is torn down for the whole lock, and the sheet re-enters intact on unlock.
        val isLocked by App.pinComponent.isLockedFlow.collectAsState()
        if (!isLocked) {
            ModalBottomSheet(
                onDismissRequest = onBack,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
                properties = modalBottomSheetProperties,
                dragHandle = null
            ) {
                entry.Content()
            }
        }
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within a [ModalBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val bottomSheetProperties = lastEntry?.metadata?.get(BOTTOM_SHEET_KEY) as? ModalBottomSheetProperties
        return bottomSheetProperties?.let { properties ->
            @Suppress("UNCHECKED_CAST")
            BottomSheetScene(
                key = lastEntry.contentKey as T,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.dropLast(1),
                entry = lastEntry,
                modalBottomSheetProperties = properties,
                skipPartiallyExpanded = lastEntry.metadata[BOTTOM_SHEET_EXPANDED_KEY] == true,
                onBack = onBack
            )
        }
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [ModalBottomSheet].
         *
         * @param modalBottomSheetProperties properties that should be passed to the containing
         * [ModalBottomSheet].
         * @param skipPartiallyExpanded opens the sheet at full height instead of the
         * half-screen stop — for tall content that would otherwise show partially.
         */
        @OptIn(ExperimentalMaterial3Api::class)
        fun bottomSheet(
            modalBottomSheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
            skipPartiallyExpanded: Boolean = false,
        ): Map<String, Any> = mapOf(
            BOTTOM_SHEET_KEY to modalBottomSheetProperties,
            BOTTOM_SHEET_EXPANDED_KEY to skipPartiallyExpanded,
        )

        internal const val BOTTOM_SHEET_KEY = "bottomsheet"
        internal const val BOTTOM_SHEET_EXPANDED_KEY = "bottomsheet_expanded"
    }
}