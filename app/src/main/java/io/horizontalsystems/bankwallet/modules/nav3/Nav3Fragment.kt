package io.horizontalsystems.bankwallet.modules.nav3

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
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
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsScreen
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
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

@Serializable
data object Terms : NavKey

@Composable
fun NavExample() {
    val resultBus = remember { ResultEventBus() }

    val backStack = rememberNavBackStack(Home)

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
                HSScaffold(title = "Nav3") {
                    Column {
                        HSButton(title = "About") {
                            backStack.add(About)
                        }

                        VSpacer(36.dp)

                        var resultString by remember { mutableStateOf("Default") }
                        ResultEffect<TermsFragment.Result>(resultBus) {
                            resultString = "termsAccepted: ${it.termsAccepted}"
                        }

                        title3_leah(resultString)
                        VSpacer(36.dp)

                        HSButton(title = "Terms") {
                            backStack.add(Terms)
                        }
                    }
                }
            }

            entry<About> {
                AboutScreen(
                    onBackPress = { backStack.removeLastOrNull() },
                    navigateToReleaseNotes = { backStack.add(ReleaseNotes) },
                    navigateToAppStatus = { backStack.add(AppStatus) },
                    navigateToTerms = { backStack.add(Terms) }
                )
            }
            entry<AppStatus> {
                AppStatusScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<ReleaseNotes> {
                ReleaseNotesScreen(false, { backStack.removeLastOrNull() })
            }
            entry<Terms> {
                TermsScreen(
                    onBack = { backStack.removeLastOrNull() },
                    setResult = {
                        resultBus.sendResult(result = it)
                    }
                )
            }
        },
    )
}