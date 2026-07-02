package io.horizontalsystems.walletkit.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

data class DataFieldRecipient(val address: Address) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        QuoteInfoRow(
            title = stringResource(R.string.Swap_Recipient),
            value = address.hex.hs(ComposeAppTheme.colors.leah)
        )
    }
}
