package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey.RestorePrivateKey
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreFromPrivateKeyPage(val input: ManageAccountsModule.Input?) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val popUpToInclusiveId = input?.popOffOnSuccess ?: RestoreFromPrivateKeyPage::class
        val inclusive = input?.popOffInclusive ?: false

        RestoreFromPrivateKeyNavHost(navigation, popUpToInclusiveId, inclusive)
    }
}

@Composable
private fun RestoreFromPrivateKeyNavHost(
    navigation: HSNavigation,
    popUpToInclusiveId: KClass<out HSPage>,
    inclusive: Boolean,
) {
    val mainViewModel: RestoreViewModel = viewModel()

    val view = LocalView.current

    val uiState = mainViewModel.uiState

    LaunchedEffect(uiState.openSelectCoinsScreen) {
        if (uiState.openSelectCoinsScreen) {
            val accountType = mainViewModel.accountType
            val statPage = mainViewModel.statPage
            if (accountType != null && statPage != null) {
                navigation.add(
                    restore_select_coins(
                        input = ManageAccountsModule.Input(popUpToInclusiveId, inclusive),
                        accountType = accountType,
                        accountName = mainViewModel.accountName,
                        manualBackup = mainViewModel.manualBackup,
                        fileBackup = mainViewModel.fileBackup,
                        statPage = statPage
                    )
                )
            }
            mainViewModel.openSelectCoinsScreenHandled()
        }
    }

    LaunchedEffect(uiState.restored) {
        if (uiState.restored) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Restored,
                icon = R.drawable.icon_add_to_wallet_2_24,
                iconTint = R.color.white
            )
            delay(300)
            navigation.removeLastUntil(popUpToInclusiveId, inclusive)
        }
    }

    RestorePrivateKey(
        mainViewModel = mainViewModel,
        openSelectNetworkScreen = {
            navigation.add(restore_select_network(ManageAccountsModule.Input(popUpToInclusiveId, inclusive)))
        },
        openSelectCoinsScreen = { mainViewModel.requestOpenSelectCoinsScreen() },
        onBackClick = { navigation.removeLastOrNull() },
    )
}

@Serializable
data class restore_select_network(val input: ManageAccountsModule.Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val mainViewModel = navigation.viewModelForScreen<RestoreViewModel>(RestoreFromPrivateKeyPage::class)

        val uiState = mainViewModel.uiState
        val view = LocalView.current

        LaunchedEffect(uiState.openSelectCoinsScreen) {
            if (uiState.openSelectCoinsScreen) {
                val accountType = mainViewModel.accountType
                val statPage = mainViewModel.statPage
                if (accountType != null && statPage != null) {
                    navigation.add(
                        restore_select_coins(
                            input = input,
                            accountType = accountType,
                            accountName = mainViewModel.accountName,
                            manualBackup = mainViewModel.manualBackup,
                            fileBackup = mainViewModel.fileBackup,
                            statPage = statPage
                        )
                    )
                }
                mainViewModel.openSelectCoinsScreenHandled()
            }
        }

        LaunchedEffect(uiState.restored) {
            if (uiState.restored) {
                HudHelper.showSuccessMessage(
                    contenView = view,
                    resId = R.string.Hud_Text_Restored,
                    icon = R.drawable.icon_add_to_wallet_2_24,
                    iconTint = R.color.white
                )
                delay(300)
                navigation.removeLastUntil(input.popOffOnSuccess, input.popOffInclusive)
            }
        }

        SelectNetworkScreen(
            onBackClick = { navigation.removeLastOrNull() },
            accountTypes = mainViewModel.accountTypes,
            mainViewModel = mainViewModel
        )
    }
}
