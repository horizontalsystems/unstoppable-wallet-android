package io.horizontalsystems.walletkit.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

// The XRPL destination tag the deposit is sent with — the counterparty's crediting identifier,
// carried by the transaction itself rather than by a memo.
data class DataFieldDestinationTag(val tag: Long) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val infoTitle = stringResource(R.string.Send_DestinationTag)
        val infoText = stringResource(R.string.Send_DestinationTag_Info)
        QuoteInfoRow(
            title = infoTitle,
            value = tag.toString().hs(ComposeAppTheme.colors.leah),
            onInfoClick = {
                navigation.slideFromBottom(SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText)))
            }
        )
    }
}
