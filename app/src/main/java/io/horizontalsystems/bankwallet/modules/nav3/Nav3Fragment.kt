package io.horizontalsystems.bankwallet.modules.nav3

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesScreen
import io.horizontalsystems.bankwallet.modules.settings.about.AboutScreen
import io.horizontalsystems.bankwallet.modules.settings.appstatus.AppStatusScreen
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsScreen
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsScreen
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.serialization.Serializable

class Nav3Fragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        NavExample()
    }

}

@Serializable
abstract class HSScreen(val screenshotEnabled: Boolean = true) : NavKey {
    open fun getMetadata() = mapOf<String, Any>()

    @Composable
    open fun GetContent(backStack: MutableList<HSScreen>, resultBus: ResultEventBus) {
        HSScaffold(title = "TODO") {

        }
    }
}

@Serializable
data object BottomSheetSample : HSScreen() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun getMetadata() = BottomSheetSceneStrategy.bottomSheet()

    @Composable
    override fun GetContent(
        backStack: MutableList<HSScreen>,
        resultBus: ResultEventBus
    ) {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.warning_filled_24),
            title = "Title"
        )
        TextBlock(
            text = "By clicking connect, you allow this app to view your public address.",
            textAlign = TextAlign.Center
        )
    }
}

@Serializable
data object Child : HSScreen() {
    override fun getMetadata() = SharedViewModelStoreNavEntryDecorator.parent(
        Home.toString()
    )

    @Composable
    override fun GetContent(
        backStack: MutableList<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val parentViewModel = viewModel(modelClass = SharedViewModel::class)
        HSScaffold(title = "Child") {
            title3_leah("uuid: " + parentViewModel.uuid)
        }
    }
}

@Serializable
data object Settings : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: MutableList<HSScreen>,
        resultBus: ResultEventBus
    ) {
        SettingsScreen(
            navigateToBuySubscription = {
//                fragmentNavController.slideFromBottom(R.id.buySubscriptionFragment)
            },
            navigateToWhyDonate = {
//                fragmentNavController.slideFromBottom(R.id.whyDonateFragment)
            },
            navigateToManageAccounts = {
//                fragmentNavController.slideFromRight(
//                    R.id.manageAccountsFragment,
//                    ManageAccountsModule.Mode.Manage
//                )
            },
            navigateToBlockchainSettings = {
//                fragmentNavController.slideFromRight(R.id.blockchainSettingsFragment)
            },
            navigateToSecuritySettings = {
//                fragmentNavController.slideFromRight(R.id.securitySettingsFragment)
            },
            navigateToPrivacySettings = {
//                fragmentNavController.slideFromRight(R.id.privacySettingsFragment)
            },
            navigateToWcList = {
//                fragmentNavController.slideFromRight(R.id.wcListFragment)
            },
            navigateToWcErrorNoAccount = {
//                fragmentNavController.slideFromBottom(R.id.wcErrorNoAccountFragment)
            },
            navigateToBackupRequired = {
//                fragmentNavController.slideFromBottom(R.id.backupRequiredDialog, it)
            },
            navigateToWcAccountTypeNotSupported = {
//                fragmentNavController.slideFromBottom(R.id.wcAccountTypeNotSupportedDialog, it)
            },
            navigateToAppearance = {
//                fragmentNavController.slideFromRight(R.id.appearanceFragment)
            },
            navigateToSubscription = {
//                fragmentNavController.slideFromRight(R.id.subscriptionFragment)
            },
            navigateToContacts = {
//                fragmentNavController.slideFromRight(
//                    R.id.contactsFragment,
//                    ContactsFragment.Input(Mode.Full)
//                )
            },
            navigateToBackupManager = {
//                fragmentNavController.slideFromRight(R.id.backupManagerFragment)
            },
            navigateToAddressCheck = {
//                fragmentNavController.slideFromRight(R.id.addressCheckFragment)
            },
            navigateToAbout = {
                backStack.add(About)
            },
            navigateToFaq = {
//                fragmentNavController.slideFromRight(R.id.faqListFragment)
            },
            navigateToAcademy = {
//                fragmentNavController.slideFromRight(R.id.academyFragment)
            },
            navigateToDonate = {
//                fragmentNavController.slideFromRight(R.id.donateTokenSelectFragment)
            },
            navigateToPaidAction = { paidAction, block ->
//                fragmentNavController.paidAction(
//                    paidAction,
//                    block
//                )
            },
        )
    }
}

@Serializable
data object Home : HSScreen() {
    @Composable
    override fun GetContent(backStack: MutableList<HSScreen>, resultBus: ResultEventBus) {
        val viewModel = viewModel(modelClass = SharedViewModel::class)

        HSScaffold(title = "Nav3") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                title3_leah("viewModel.uuid: " + viewModel.uuid)

                HSButton(title = "Child") {
                    backStack.add(Child)
                }

                HSButton(title = "Bottom Sheet") {
                    backStack.add(BottomSheetSample)
                }

                var resultString by remember { mutableStateOf("Default") }
                ResultEffect<TermsFragment.Result>(resultBus) {
                    resultString = "termsAccepted: ${it.termsAccepted}"
                }

                title3_leah(resultString)

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
        NavBackStack<HSScreen>(Settings)
    }

    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<HSScreen>() }

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
            rememberViewModelStoreNavEntryDecorator(),
            rememberSharedViewModelStoreNavEntryDecorator(),
        ),
        backStack = backStack,
        sceneStrategy = bottomSheetStrategy,
        entryProvider = { hSScreen ->
            NavEntry(hSScreen, metadata = hSScreen.getMetadata()) {
                hSScreen.GetContent(backStack, resultBus)
            }
        }
    )
}