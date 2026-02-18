package io.horizontalsystems.bankwallet.modules.nav3

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
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
abstract class HSScreen(val screenshotEnabled: Boolean = true) : NavKey {
    @Composable
    open fun GetContent(backStack: MutableList<HSScreen>, resultBus: ResultEventBus) {
        HSScaffold(title = "TODO") {

        }
    }
}

@Serializable
data object Home : HSScreen() {
    @Composable
    override fun GetContent(backStack: MutableList<HSScreen>, resultBus: ResultEventBus) {
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
}

@Serializable
data object About : HSScreen() {
    @Composable
    override fun GetContent(backStack: MutableList<HSScreen>, resultBus: ResultEventBus) {
        AboutScreen(
            onBackPress = { backStack.removeLastOrNull() },
            navigateToReleaseNotes = { backStack.add(ReleaseNotes) },
            navigateToAppStatus = { backStack.add(AppStatus) },
            navigateToTerms = { backStack.add(Terms) }
        )
    }
}

@Serializable
data object AppStatus : HSScreen() {
    @Composable
    override fun GetContent(backStack: MutableList<HSScreen>, resultBus: ResultEventBus) {
        AppStatusScreen(onBack = { backStack.removeLastOrNull() })
    }
}

@Serializable
data object ReleaseNotes : HSScreen() {
    @Composable
    override fun GetContent(backStack: MutableList<HSScreen>, resultBus: ResultEventBus) {
        ReleaseNotesScreen(false, { backStack.removeLastOrNull() })
    }
}

@Serializable
data object Terms : HSScreen() {
    @Composable
    override fun GetContent(backStack: MutableList<HSScreen>, resultBus: ResultEventBus) {
        TermsScreen(
            onBack = { backStack.removeLastOrNull() },
            setResult = {
                resultBus.sendResult(result = it)
            }
        )
    }
}

@Composable
fun NavExample() {
    val resultBus = remember { ResultEventBus() }

    val backStack = rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        NavBackStack<HSScreen>(About)
    }

    val currentScreen = backStack.lastOrNull()
    val activity = LocalActivity.current

    LaunchedEffect(currentScreen) {
        if (activity != null) {
            if (currentScreen?.screenshotEnabled == false) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    NavDisplay(
        entryDecorators = listOf(
            // Add the default decorators for managing scenes and saving state
            rememberSaveableStateHolderNavEntryDecorator(),
            // Then add the view model store decorator
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        entryProvider = { key ->
            NavEntry(key) {
                key.GetContent(backStack, resultBus)
            }
        }
    )
}