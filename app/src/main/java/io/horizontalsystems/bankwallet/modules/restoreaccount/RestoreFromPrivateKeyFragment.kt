package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.NavigationType
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey.RestorePrivateKey
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreFromPrivateKeyFragment(val input: ManageAccountsModule.Input?) : HSScreen(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val popUpToInclusiveId = input?.popOffOnSuccess ?: RestoreFromPrivateKeyFragment::class
        val inclusive = input?.popOffInclusive ?: false

        RestoreFromPrivateKeyNavHost(navController, popUpToInclusiveId, inclusive)
    }
}

@Composable
private fun RestoreFromPrivateKeyNavHost(
    navController: HSNavigation,
    popUpToInclusiveId: KClass<out HSScreen>,
    inclusive: Boolean,
) {
    val mainViewModel: RestoreViewModel = viewModel()

    val view = LocalView.current

    val uiState = mainViewModel.uiState

    LaunchedEffect(uiState.openSelectCoinsScreen) {
        if (uiState.openSelectCoinsScreen) {
            mainViewModel.openSelectCoinsScreenHandled()

            val accountType = mainViewModel.accountType
            val statPage = mainViewModel.statPage
            if (accountType != null && statPage != null) {
                navController.add(
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
            navController.removeLastUntil(popUpToInclusiveId, inclusive)
        }
    }

    RestorePrivateKey(
        mainViewModel = mainViewModel,
        openSelectNetworkScreen = navController.slideForResult<AccountType>(
            navigationType = NavigationType.SlideFromRight,
            screenBuilder = { restore_select_network(mainViewModel.accountTypes) }
        ) {
            mainViewModel.setAccountType(it)
            mainViewModel.requestOpenSelectCoinsScreen()
        },
        openSelectCoinsScreen = { mainViewModel.requestOpenSelectCoinsScreen() },
        onBackClick = { navController.removeLastOrNull() },
    )
}

@Serializable
data class restore_select_network(val accountTypes: List<AccountType>) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        SelectNetworkScreen(
            onBackClick = { navController.removeLastOrNull() },
            accountTypes = accountTypes
        )
    }
}
