package io.horizontalsystems.walletkit.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

// The provider-supplied address the deposit transaction pays. Shown so the user can see (and
// verify) where their funds actually go, not just the recipient of the swapped output.
data class DataFieldDepositAddress(val address: String) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val infoTitle = stringResource(R.string.Swap_DepositAddress)
        val infoText = stringResource(R.string.Swap_DepositAddress_Description)
        QuoteInfoRow(
            title = infoTitle,
            value = address.hs(ComposeAppTheme.colors.leah),
            onInfoClick = {
                navigation.slideFromBottom(SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText)))
            }
        )
    }
}
