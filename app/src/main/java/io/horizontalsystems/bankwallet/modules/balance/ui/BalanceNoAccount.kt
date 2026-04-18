package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.navigateWithTermsAccepted
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsErrorMessageDefault

@Composable
fun BalanceNoAccount(navController: NavController) {
    HSScaffold(
        title = stringResource(R.string.Wallet_Title)
    ) {
        CardsErrorMessageDefault(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp),
            icon = painterResource(R.drawable.wallet_add_24),
            iconTint = ComposeAppTheme.colors.grey,
            buttonTitle = stringResource(R.string.ManageAccounts_CreateNewWallet),
            buttonTitle2 = stringResource(R.string.ManageAccounts_ImportWallet),
            buttonTitle3 = stringResource(R.string.ManageAccounts_WatchAddress),
            onClick = {
                navController.navigateWithTermsAccepted {
                    navController.slideFromRight(R.id.createAccountFragment)

                    stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.NewWallet))
                }
            },
            onClick2 = {
                navController.navigateWithTermsAccepted {
                    navController.slideFromRight(R.id.importWalletFragment)

                    stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.ImportWallet))
                }
            },
            onClick3 = {
                navController.slideFromRight(R.id.watchAddressFragment)

                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.WatchWallet))
            }
        )
    }
}
