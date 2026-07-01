package io.horizontalsystems.bankwallet.modules.nav3

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
import io.horizontalsystems.bankwallet.core.NavigationType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
abstract class HSPage(
    val bottomSheet: Boolean = false,
    val screenshotEnabled: Boolean = true,
    var resultKey: String? = null,
    val uuid: String = UUID.randomUUID().toString(),
    var navType: NavigationType = NavigationType.SlideFromRight,
) : NavKey {

    fun contentKey() = this::class.simpleName ?: "HSScreen"

    @OptIn(ExperimentalMaterial3Api::class)
    fun getMetadata() = buildMap {
        if (bottomSheet) {
            putAll(BottomSheetSceneStrategy.bottomSheet())
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