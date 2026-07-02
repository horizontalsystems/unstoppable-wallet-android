package io.horizontalsystems.core.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.core.entities.Address
import io.horizontalsystems.core.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.uiv3.components.cell.hs

data class DataFieldRecipient(val address: Address) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        QuoteInfoRow(
            title = stringResource(R.string.Swap_Recipient),
            value = address.hex.hs(ComposeAppTheme.colors.leah)
        )
    }
}
