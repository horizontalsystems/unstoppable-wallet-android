package io.horizontalsystems.core.modules.send.bitcoin.advanced

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.core.R
import io.horizontalsystems.core.modules.info.ui.InfoBody
import io.horizontalsystems.core.modules.info.ui.InfoHeader
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.ui.compose.TranslatableString
import io.horizontalsystems.core.ui.compose.components.MenuItem
import io.horizontalsystems.core.uiv3.components.HSScaffold

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
