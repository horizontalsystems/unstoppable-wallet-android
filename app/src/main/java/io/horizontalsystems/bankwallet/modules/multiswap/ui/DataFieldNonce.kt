package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

data class DataFieldNonce(val nonce: Long) : DataField {
    @Composable
    override fun GetContent(navController: NavController) {
        QuoteInfoRow(
            title = stringResource(R.string.Send_Confirmation_Nonce),
            value = nonce.toString().hs
        )
    }
}