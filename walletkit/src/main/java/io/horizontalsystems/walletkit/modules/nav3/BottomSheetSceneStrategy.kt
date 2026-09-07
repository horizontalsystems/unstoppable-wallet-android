package io.horizontalsystems.walletkit.modules.nav3

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.modules.nav3.BottomSheetSceneStrategy.Companion.bottomSheet
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

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

    // The state of the sheet currently on screen, so onRemove can animate it out. Null while the
    // sheet is not composed (see the lock gate below).
    private var sheetState: SheetState? = null

    override val content: @Composable (() -> Unit) = {
        // ModalBottomSheet opens its own window, composited above the activity window — and so
        // above the PinUnlock overlay, which would leave the sheet interactable over the keypad.
        // The lock also engages asynchronously (PinComponent collects background state on a
        // Default-dispatcher coroutine), so a sheet can slip onto the stack in the same frame the
        // app locks. Keep the entry on the stack but don't compose the sheet while locked: the
        // window is torn down for the whole lock, and the sheet re-enters intact on unlock.
        // Keyed on the keypad's visibility, not the raw lock: market sheets (filters, etc.) stay
        // usable while browsing the Market tab locked.
        val showUnlock by App.lockGate.showUnlockFlow.collectAsStateWithLifecycle()
        if (!showUnlock) {
            val state = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
            DisposableEffect(state) {
                sheetState = state
                onDispose {
                    if (sheetState === state) sheetState = null
                }
            }
            val dismissRegistry = remember { BottomSheetDismissRegistry() }
            ModalBottomSheet(
                onDismissRequest = {
                    val handler = dismissRegistry.handler
                    if (handler != null) handler() else onBack()
                },
                sheetState = state,
                containerColor = ComposeAppTheme.colors.lawrence,
                properties = modalBottomSheetProperties,
                dragHandle = null
            ) {
                CompositionLocalProvider(LocalBottomSheetDismissRegistry provides dismissRegistry) {
                    entry.Content()
                }
            }
        }
    }

    // NavDisplay keeps a popped overlay scene composed until this returns, so the sheet and its
    // scrim animate out on every pop, not only on the swipe/scrim-tap paths the sheet drives
    // itself. A pop that follows one of those finds the sheet already hidden and returns at once.
    override suspend fun onRemove() {
        val state = sheetState ?: return
        try {
            state.hide()
        } catch (e: CancellationException) {
            // A drag or another sheet animation interrupted the hide; the scene still has to go.
            if (!currentCoroutineContext().isActive) throw e
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BottomSheetScene<*>

        return key == other.key &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries &&
            entry == other.entry &&
            modalBottomSheetProperties == other.modalBottomSheetProperties &&
            skipPartiallyExpanded == other.skipPartiallyExpanded
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
            previousEntries.hashCode() * 31 +
            overlaidEntries.hashCode() * 31 +
            entry.hashCode() * 31 +
            modalBottomSheetProperties.hashCode() * 31 +
            skipPartiallyExpanded.hashCode()
    }

    override fun toString(): String {
        return "BottomSheetScene(key=$key, entry=$entry, previousEntries=$previousEntries, overlaidEntries=$overlaidEntries)"
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
