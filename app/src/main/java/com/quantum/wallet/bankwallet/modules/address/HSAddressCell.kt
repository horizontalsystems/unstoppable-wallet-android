package com.quantum.wallet.bankwallet.modules.address

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.cell.CellUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun HSAddressCell(
    title: String,
    value: String,
    riskyAddress: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (riskyAddress) {
        ComposeAppTheme.colors.red50
    } else {
        ComposeAppTheme.colors.transparent
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence),
        content = {
            CellUniversal(
                borderTop = false,
                onClick = onClick
            ) {
                subhead2_grey(text = title)

                HSpacer(16.dp)
                subhead1_leah(
                    modifier = Modifier.weight(1f),
                    text = value
                )
                if (riskyAddress) {
                    HSpacer(16.dp)
                    Icon(
                        painter = painterResource(id = R.drawable.ic_attention_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.lucian
                    )
                }

                HSpacer(16.dp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    )
}