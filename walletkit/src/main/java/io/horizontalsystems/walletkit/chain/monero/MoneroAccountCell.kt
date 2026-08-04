package io.horizontalsystems.walletkit.chain.monero

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IMoneroAccountsAdapter
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.balance.token.moneroaccounts.MoneroAccountsPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.BoxBordered
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

@Composable
internal fun MoneroAccountCell(
    wallet: Wallet,
    navigation: HSNavigation
) {
    val adapter = remember(wallet) {
        App.adapterManager.getAdapterForWallet<IMoneroAccountsAdapter>(wallet)
    } ?: return
    val activeAccountIndex by adapter.activeAccountFlow.collectAsState()
    val accounts by adapter.accountsFlow.collectAsState()
    val label = accounts.firstOrNull { it.index == activeAccountIndex }
        ?.let { "${it.index}. ${it.label}" }
        ?: "$activeAccountIndex."

    BoxBordered(bottom = true) {
        CellPrimary(
            middle = {
                CellMiddleInfo(eyebrow = stringResource(R.string.Monero_Account).hs)
            },
            right = {
                CellRightNavigation(
                    subtitle = label.hs(color = ComposeAppTheme.colors.leah)
                )
            },
            backgroundColor = ComposeAppTheme.colors.lawrence,
            onClick = {
                navigation.slideFromRight(
                    MoneroAccountsPage(MoneroAccountsPage.Input(wallet))
                )
            }
        )
    }
}
