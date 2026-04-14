package com.quantum.wallet.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.modules.multiswap.QuoteInfoRow
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs

data class DataFieldNonce(val nonce: Long) : DataField {
    @Composable
    override fun GetContent(navController: NavController) {
        QuoteInfoRow(
            title = stringResource(R.string.Send_Confirmation_Nonce),
            value = nonce.toString().hs(ComposeAppTheme.colors.leah)
        )
    }
}