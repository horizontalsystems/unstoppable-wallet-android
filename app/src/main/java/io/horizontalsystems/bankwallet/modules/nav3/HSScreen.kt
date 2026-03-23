package io.horizontalsystems.bankwallet.modules.nav3

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
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
    val uuid = UUID.randomUUID().toString()

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
    }

    private val className = this.javaClass.simpleName

    @Composable
    open fun GetContent(navController: NavBackStack<HSScreen>) {
        HSScaffold(title = "TODO") {
            body_leah(className)
        }
    }
}