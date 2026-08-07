package io.horizontalsystems.walletkit.modules.send.bitcoin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_leah

@Composable
fun UtxoCell(
    utxoData: UtxoData,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        onClick = onClick
    ) {
        subhead2_grey(
            text = stringResource(R.string.Send_Utxos),
            modifier = Modifier.weight(1f)
        )
        subhead2_leah(text = utxoData.value)
        HSpacer(8.dp)
        when (utxoData.type) {
            UtxoType.Auto -> {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }

            UtxoType.Manual -> {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.jacob
                )
            }

            null -> {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
