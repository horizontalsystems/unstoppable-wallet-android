package io.horizontalsystems.core.modules.balance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.NavigationType
import io.horizontalsystems.core.core.stats.StatEvent
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.stat
import io.horizontalsystems.core.modules.createaccount.CreateAccountPage
import io.horizontalsystems.core.modules.importwallet.ImportWalletPage
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.watchaddress.WatchAddressPage
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.uiv3.components.HSScaffold

@Composable
fun BalanceNoAccount(navigation: HSNavigation) {
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
                    navigation.navigateWithTermsAccepted(
                        screen = CreateAccountPage(),
                        navigationType = NavigationType.SlideFromRight,
                        statPageFrom = StatPage.Balance,
                        statPageTo = StatPage.NewWallet
                    )
                },
                onWalletRestoreClick = {
                    navigation.navigateWithTermsAccepted(
                        screen = ImportWalletPage(),
                        navigationType = NavigationType.SlideFromRight,
                        statPageFrom = StatPage.Balance,
                        statPageTo = StatPage.ImportWallet
                    )
                },
                onWatchWalletClick = {
                    navigation.slideFromRight(WatchAddressPage())

                    stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.WatchWallet))
                }
            )
        }
    }
}
