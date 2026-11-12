package io.horizontalsystems.walletkit.modules.manageaccounts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.modules.balance.ui.AddWalletView
import io.horizontalsystems.walletkit.modules.createaccount.CreateAccountPage
import io.horizontalsystems.walletkit.modules.importwallet.ImportWalletPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.watchaddress.WatchAddressPage
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class AddWalletPage(val input: ManageAccountsModule.Input?) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AddWalletScreen(navigation, input)
    }
}

@Composable
fun AddWalletScreen(navigation: HSNavigation, input: ManageAccountsModule.Input?) {
    HSScaffold(
        title = stringResource(R.string.ManageAccounts_AddWallet),
        onBack = navigation::removeLastOrNull,
    ) {
        AddWalletView(
            onNewWalletClick = {
                navigation.slideFromRight(CreateAccountPage(input))
                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.NewWallet))
            },
            onWalletRestoreClick = {
                navigation.slideFromRight(ImportWalletPage(input))
                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.ImportWallet))
            },
            onWatchWalletClick = {
                navigation.slideFromRight(WatchAddressPage(input))

                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.WatchWallet))
            }
        )
    }
}