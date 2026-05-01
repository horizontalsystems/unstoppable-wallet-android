package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.NavigationType
import io.horizontalsystems.bankwallet.core.addFromRight
import io.horizontalsystems.bankwallet.core.navigateWithTermsAccepted
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.createaccount.CreateAccountFragment
import io.horizontalsystems.bankwallet.modules.importwallet.ImportWalletFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.watchaddress.WatchAddressFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

@Composable
fun BalanceNoAccount(navController: NavBackStack<HSScreen>) {
    HSScaffold(
        title = stringResource(R.string.Wallet_Title)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp) //compensate bottom navigation height, to center content
        ) {
            AddWalletView(
                modifier = Modifier
                    .align(Alignment.Center),
                icon = painterResource(R.drawable.wallet_add_24),
                iconTint = ComposeAppTheme.colors.grey,
                onNewWalletClick = {
                    navController.navigateWithTermsAccepted(
                        screen = CreateAccountFragment(),
                        navigationType = NavigationType.SlideFromRight,
                        statPageFrom = StatPage.Balance,
                        statPageTo = StatPage.NewWallet
                    )
                },
                onWalletRestoreClick = {
                    navController.navigateWithTermsAccepted(
                        screen = ImportWalletFragment(),
                        navigationType = NavigationType.SlideFromRight,
                        statPageFrom = StatPage.Balance,
                        statPageTo = StatPage.ImportWallet
                    )
                },
                onWatchWalletClick = {
                    navController.addFromRight(WatchAddressFragment())

                    stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.WatchWallet))
                }
            )
        }
    }
}
