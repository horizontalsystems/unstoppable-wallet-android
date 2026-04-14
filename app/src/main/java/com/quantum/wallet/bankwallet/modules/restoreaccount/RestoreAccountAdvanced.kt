package com.quantum.wallet.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import com.quantum.wallet.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuModule.RestoreOption
import com.quantum.wallet.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import com.quantum.wallet.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import com.quantum.wallet.bankwallet.modules.restoreaccount.restoreprivatekey.RestorePrivateKey

@Composable
fun AdvancedRestoreScreen(
    restoreMenuViewModel: RestoreMenuViewModel,
    mainViewModel: RestoreViewModel,
    openSelectNetworkScreen: () -> Unit,
    openSelectCoinsScreen: () -> Unit,
    openNonStandardRestore: () -> Unit,
    onBackClick: () -> Unit,
) {
    when (restoreMenuViewModel.restoreOption) {
        RestoreOption.RecoveryPhrase -> {
            RestorePhrase(
                advanced = true,
                restoreMenuViewModel = restoreMenuViewModel,
                mainViewModel = mainViewModel,
                openSelectCoins = openSelectCoinsScreen,
                openNonStandardRestore = openNonStandardRestore,
                onBackClick = onBackClick,
            )
        }
        RestoreOption.PrivateKey -> {
            RestorePrivateKey(
                restoreMenuViewModel = restoreMenuViewModel,
                mainViewModel = mainViewModel,
                openSelectNetworkScreen = openSelectNetworkScreen,
                openSelectCoinsScreen = openSelectCoinsScreen,
                onBackClick = onBackClick,
            )
        }
    }
}
