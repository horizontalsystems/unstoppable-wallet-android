package io.horizontalsystems.walletkit.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

data class DataFieldNonce(val nonce: Long) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        QuoteInfoRow(
            title = stringResource(R.string.Send_Confirmation_Nonce),
            value = nonce.toString().hs(ComposeAppTheme.colors.leah)
        )
    }
}