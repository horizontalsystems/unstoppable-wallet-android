package io.horizontalsystems.bankwallet.modules.nav3

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesScreen
import io.horizontalsystems.bankwallet.modules.settings.about.AboutScreen
import io.horizontalsystems.bankwallet.modules.settings.appstatus.AppStatusScreen
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import kotlinx.serialization.Serializable

class Nav3Fragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        NavExample()
    }

}


@Serializable
data object Home : NavKey

@Serializable
data object About : NavKey

@Serializable
data object AppStatus : NavKey

@Serializable
data object ReleaseNotes : NavKey


@Composable
fun NavExample() {
    val backStack = rememberNavBackStack(About)

    NavDisplay(
        entryDecorators = listOf(
            // Add the default decorators for managing scenes and saving state
            rememberSaveableStateHolderNavEntryDecorator(),
            // Then add the view model store decorator
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Home> {
                HSButton(title = "Home") {
                    backStack.add(AppStatus)
                }
            }

            entry<About> {
                AboutScreen(
                    onBackPress = { backStack.removeLastOrNull() },
                    navigateToReleaseNotes = { backStack.add(ReleaseNotes) },
                    navigateToAppStatus = { backStack.add(AppStatus) },
                    navigateToTerms = { /*backStack.add(AppStatus)*/ }
                )
            }
            entry<AppStatus> {
                AppStatusScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<ReleaseNotes> {
                ReleaseNotesScreen(false, { backStack.removeLastOrNull() })
            }
        },
    )
}