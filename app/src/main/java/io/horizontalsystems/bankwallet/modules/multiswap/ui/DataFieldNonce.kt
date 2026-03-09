package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

data class DataFieldNonce(val nonce: Long) : DataField {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        QuoteInfoRow(
            title = stringResource(R.string.Send_Confirmation_Nonce),
            value = nonce.toString().hs(ComposeAppTheme.colors.leah)
        )
    }
}