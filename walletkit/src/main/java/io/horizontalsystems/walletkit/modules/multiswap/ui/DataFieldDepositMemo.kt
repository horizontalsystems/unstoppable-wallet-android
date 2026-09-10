package io.horizontalsystems.walletkit.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

// The provider-supplied memo embedded in the deposit transaction (THORChain routing memo, or a
// P2P provider's order identifier).
data class DataFieldDepositMemo(val memo: String) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val infoTitle = stringResource(R.string.Swap_DepositMemo)
        val infoText = stringResource(R.string.Swap_DepositMemo_Description)
        QuoteInfoRow(
            title = infoTitle,
            value = memo.hs(ComposeAppTheme.colors.leah),
            onInfoClick = {
                navigation.slideFromBottom(SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText)))
            }
        )
    }
}
