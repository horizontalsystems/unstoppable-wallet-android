package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun HSAddressCell(title: String, value: String, onClick: () -> Unit) {
    SectionUniversalLawrence {
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

            HSpacer(16.dp)
            Icon(
                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
    }
}