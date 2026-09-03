package io.horizontalsystems.walletkit.modules.nav3

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import io.horizontalsystems.walletkit.core.NavigationType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
abstract class HSPage(
    val bottomSheet: Boolean = false,
    // opens the sheet at full height instead of the half-screen stop —
    // for tall sheet content that would otherwise show partially
    val expandedBottomSheet: Boolean = false,
    val screenshotEnabled: Boolean = true,
    // May be shown while the app is PIN-locked (read-only market content). Pushing a page
    // without this while locked shows the keypad first — see LockGate.
    val accessibleWhileLocked: Boolean = false,
    var resultKey: String? = null,
    val uuid: String = UUID.randomUUID().toString(),
    var navType: NavigationType = NavigationType.SlideFromRight,
) : NavKey {

    // Identifies the entry's content: NavDisplay, the saveable state holder and the per-entry
    // ViewModel store are all keyed by it. Pages that can be replaced by another instance of
    // the same class in one frame (e.g. a coin page by a widget tap) must include what makes
    // the instance distinct, or the old content and its ViewModel stay on screen.
    open fun contentKey(): String = this::class.simpleName ?: "HSScreen"

    @OptIn(ExperimentalMaterial3Api::class)
    fun getMetadata() = buildMap {
        if (bottomSheet) {
            putAll(BottomSheetSceneStrategy.bottomSheet(skipPartiallyExpanded = expandedBottomSheet))
        }

        putAll(getAnimationMetadata())
    }

    private fun getAnimationMetadata() = buildMap {
        when (navType) {
            NavigationType.SlideFromBottom -> {
                putAll(
                    NavDisplay.transitionSpec {
                        slideInVertically(tween(300)) { it } togetherWith fadeOut(
                            tween(400)
                        )
                    }
                )

                putAll(
                    NavDisplay.popTransitionSpec {
                        fadeIn(tween(500)) togetherWith slideOutVertically(
                            tween(300)
                        ) { it }
                    }
                )
            }

            NavigationType.SlideFromRight -> {
                putAll(
                    NavDisplay.transitionSpec {
                        slideInHorizontally(tween(300)) { it } togetherWith fadeOut(
                            tween(400)
                        )
                    }
                )
                putAll(
                    NavDisplay.popTransitionSpec {
                        fadeIn(tween(500)) togetherWith slideOutHorizontally(
                            tween(300)
                        ) { it }
                    }
                )
            }
        }

    }

    @Composable
    abstract fun GetContent(navigation: HSNavigation)
}