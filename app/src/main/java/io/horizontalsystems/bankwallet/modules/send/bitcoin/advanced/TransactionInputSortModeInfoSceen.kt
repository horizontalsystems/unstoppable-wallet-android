package io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.info.ui.InfoBody
import io.horizontalsystems.bankwallet.modules.info.ui.InfoHeader
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

@Composable
fun BtcTransactionInputSortInfoScreen(
    onCloseClick: () -> Unit
) {
    ComposeAppTheme {
        Surface(color = ComposeAppTheme.colors.tyler) {
            Column {
                AppBar(
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onCloseClick
                        )
                    )
                )

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
}
