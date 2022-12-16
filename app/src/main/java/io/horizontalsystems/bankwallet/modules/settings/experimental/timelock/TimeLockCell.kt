package io.horizontalsystems.bankwallet.modules.settings.experimental.timelock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.B2
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal

@Composable
fun TimeLockCell(openTimeLock: () -> Unit) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = openTimeLock
            ) {
                B2(
                    text = stringResource(R.string.ExperimentalFeatures_BitcoinHodling),
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                )
            }
        }
    )
}
