package com.quantum.wallet.bankwallet.modules.send.bitcoin.advanced

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.modules.info.ui.InfoBody
import com.quantum.wallet.bankwallet.modules.info.ui.InfoHeader
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

@Composable
fun BtcTransactionInputSortInfoScreen(
    onCloseClick: () -> Unit
) {
    ComposeAppTheme {
        HSScaffold(
            title = "",
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = onCloseClick
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoHeader(R.string.BtcBlockchainSettings_TransactionInputsOutputs)
                InfoBody(R.string.BtcBlockchainSettings_TransactionInputsOutputsDescription)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
