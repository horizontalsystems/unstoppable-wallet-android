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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import io.horizontalsystems.bankwallet.core.NavigationType
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.reflect.KClass

@Serializable
abstract class HSScreen(
    val bottomSheet: Boolean = false,
    val screenshotEnabled: Boolean = true,
    val parentScreenClass: KClass<out HSScreen>? = null,
    val usePreviousScreenVmScope: Boolean = false,
) : NavKey {
    var resultKey: String? = null
    val uuid = UUID.randomUUID().toString()
    var navType = NavigationType.SlideFromRight

    fun contentKey() = "$className(#$uuid)"

    @OptIn(ExperimentalMaterial3Api::class)
    fun getMetadata(backStack: NavBackStack<HSScreen>) = buildMap {
        if (bottomSheet) {
            putAll(BottomSheetSceneStrategy.bottomSheet())
        }
        if (usePreviousScreenVmScope) {
            backStack.getOrNull(backStack.lastIndex - 1)?.let { parentScreen ->
                putAll(
                    SharedViewModelStoreNavEntryDecorator.parent(parentScreen.contentKey())
                )
            }
        }
        parentScreenClass?.let {
            backStack.findLast {
                it::class == parentScreenClass
            }?.let { parentScreen ->
                putAll(
                    SharedViewModelStoreNavEntryDecorator.parent(parentScreen.contentKey())
                )
            }
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

    private val className = this.javaClass.simpleName

    @Composable
    open fun GetContent(navController: NavBackStack<HSScreen>) {
        HSScaffold(title = "TODO") {
            body_leah(className)
        }
    }
}