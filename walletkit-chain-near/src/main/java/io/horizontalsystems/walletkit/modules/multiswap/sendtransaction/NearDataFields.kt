package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

/** NEAR a token send pays the token contract to register a receiver that does not hold the token yet. */
data class DataFieldNearReceiverRegistration(val formattedValue: String) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        QuoteInfoRow(
            title = stringResource(R.string.NearSend_ReceiverRegistration),
            value = formattedValue.hs(ComposeAppTheme.colors.leah),
        )
    }
}
